# 数字钱包平台 - 第三方接入文档（V2 API）

本文档面向接入方，描述如何通过 **V2 API** 与数字钱包平台集成，包括鉴权方式、接口列表、请求与响应格式

- [数字钱包平台 - 第三方接入文档（V2 API）](#数字钱包平台---第三方接入文档v2-api)
  - [接入流程建议](#接入流程建议)
  - [鉴权说明](#鉴权说明)
    - [API Key](#api-key)
    - [签名校验](#签名校验)
  - [V2 接口概览](#v2-接口概览)
    - [通用响应结构](#通用响应结构)
    - [代币合约地址约定](#代币合约地址约定)
  - [充值（Deposit）](#充值deposit)
    - [创建充值 / 获取地址](#创建充值--获取地址)
    - [充值订单列表](#充值订单列表)
    - [查询充值订单](#查询充值订单)
    - [取消充值订单](#取消充值订单)
  - [地址列表](#地址列表)
  - [提现（Withdraw）](#提现withdraw)
    - [创建提现](#创建提现)
    - [提现记录列表](#提现记录列表)
    - [查询提现](#查询提现)
    - [取消提现](#取消提现)
    - [查询余额](#查询余额)
  - [回调说明](#回调说明)
    - [充值回调](#充值回调)
      - [BTC 充值 status 说明](#btc-充值-status-说明)
    - [提现回调](#提现回调)
    - [回调客户返回值](#回调客户返回值)
  - [客户地址监控（Tron）](#客户地址监控tron)
    - [查询监控列表](#查询监控列表)
    - [监控回调](#监控回调)
  - [Telegram 通知接入](#telegram-通知接入)
    - [获取 Bot Token](#获取-bot-token)
    - [获取通知群的 Chat ID](#获取通知群的-chat-id)
  - [错误码说明](#错误码说明)
  - [测试代币领取说明](#测试代币领取说明)

---

## 接入流程建议

* 平台会提供后台账号，登录后台创建商户，配置**充值回调 URL**、**提现回调 URL**，添加后会生成 **API Key** 和 **Signature Key**。
  ![商户信息](./merchant.jpg) 
* 拿到 **API Key** 和 **Signature Key** 后，请参数 **鉴权说明**[#鉴权说明]。
* **充值**：调用 [创建充值](#创建充值--获取地址) 获取收款地址（可带订单信息），用户向该地址充值，链上确认后平台会调用充值回调。
* **提现**：调用 [创建提现](#创建提现) 提交提现，链上确认后平台会调用提现回调。
* 平台通知（如提现余额不足、回调异常等）可通过 [Telegram 通知](#telegram-通知接入) 接收，建议配置。

---

## 鉴权说明

### API Key

所有接口均需在请求头中携带商户 API Key：

| Header 名称 | 说明                                       |
| ----------- | ------------------------------------------ |
| `apikey`    | 在后台「商户管理」中获取，用于标识商户身份 |

示例：`apikey: your_merchant_api_key_here`

### 签名校验

* 获取**签名字符串**: 用**签名密钥（Signature Key）**与**请求 body 原文**做 **HMAC SHA256**，得到十六进制**签名字符串**
* api请求: 将 **签名字符串** 设置到请求头**X-Signature**中，如果签名校验不对，服务器会返回 14004 错误码。(目前只校验POST请求，GET请求不会校验)
* 回调签名检验: 将 **签名字符串** 与回调请求头**X-Signature**对比，如果不匹配，则不是合法的服务器请求。

示例（Python）：
```python
import hmac, hashlib
data = bytes(request_body_string, 'utf-8')
key = bytes(signing_key, 'utf-8')
digest = hmac.new(key, data, digestmod=hashlib.sha256).hexdigest()
# digest 应与 Header X-Signature 一致
```

> **Java 示例：创建充值订单并签名**

你可以参考下方 `DepositCreateTest.java` 提供的 Java 示例，进行 POST 签名请求。该示例会向 `/v2/deposit/create` 发起创建充值订单的请求，并使用 HMAC-SHA256 对请求体签名。

> **完整代码参考：** [`DepositCreateTest.java`](./DepositCreateTest.java)


**要点说明：**
- `SIGN_KEY` 和 `API_KEY`、`URL` 请替换为你在后台获取的真实值。
- 构造参数及 JSON 格式需与平台接口一致。
- 签名方式需严格使用 HMAC-SHA256，内容为原始 body 串，编码为 UTF-8。
- 签名结果写入请求头 `X-Signature`，API Key 写入 `apikey`。
- Content-Type 需为 `text/plain`。

> 如需其他语言示例（如 Python），可参考上文签名校验部分，也可联系我们获取更多 SDK 示例。


---

## V2 接口概览

基础路径：**`/v2`**

| 分类 | 方法 | 路径                   | 说明                          |
| ---- | ---- | ---------------------- | ----------------------------- |
| 充值 | POST | `/v2/deposit/create`   | 创建充值/获取地址（可选订单） |
| 充值 | GET  | `/v2/deposit/list`     | 充值订单列表（分页）          |
| 充值 | GET  | `/v2/deposit/get`      | 按 orderID 查询充值订单       |
| 充值 | POST | `/v2/deposit/cancel`   | 取消充值订单                  |
| 地址 | GET  | `/v2/address/list`     | 地址列表（按链分页）          |
| 提现 | POST | `/v2/withdraw/create`  | 创建提现                      |
| 提现 | GET  | `/v2/withdraw/list`    | 提现记录列表（按链分页）      |
| 提现 | GET  | `/v2/withdraw/get`     | 按chain 和 orderID 查询提现   |
| 提现 | POST | `/v2/withdraw/cancel`  | 取消提现                      |
| 提现 | GET  | `/v2/withdraw/balance` | 查询提现地址余额              |
| 监控 | GET  | `/v2/customer-addr-monitor/list` | 按 accountID 查询客户地址监控（目前仅 tron） |

### 通用响应结构

接口统一返回 JSON，所有响应均包含 `code` 与 `msg`；成功时另有 `data` 承载业务数据。

**成功：** `{ "code": 0, "msg": "success", "data": { ... } }`  
**失败：** `{ "code": 10001, "msg": "错误描述" }`

- `code`：0 表示成功，非 0 见 [错误码说明](#错误码说明)。
- `msg`：成功时为 `"success"`，失败时为具体描述。
- `data`：成功时按接口约定返回；失败时通常无或为空。

### 代币合约地址约定

**目标代币**。除各链真实的 ERC20 / TRC20 / Jetton 等合约地址外，以下取值表示**该链原生代币**

| 链 (`chain`) | `contractAddr` 取值 | 含义 |
| ------------ | ------------------- | ---- |
| `tron` | `T000000000000000000000000000000000` 或**空字符串** | 原生 **TRX** |
| `eth` | `0x0000000000000000000000000000000000000000` 或**空字符串** | 原生 **ETH** |
| `ton` | `__TON_NATIVE__` 或**空字符串** | 原生 **TON** |

---

## 充值（Deposit）

### 创建充值 / 获取地址

**POST** `/v2/deposit/create`

获取或创建一条与 `accountID` 绑定的充值地址；可选传入 `order` 创建充值订单（金额、orderID、过期时间等），便于后续按订单查询与回调关联。

**请求体（JSON）：**

| 参数      | 类型   | 必填 | 说明                                        |
| --------- | ------ | ---- | ------------------------------------------- |
| chain     | string | 是   | 链：`eth`、`tron` 或 `ton`                  |
| accountID | string | 是   | 账户ID。相同的accountID生成的地址会是一样的 |
| order     | object | 否   | 订单信息；不传则仅返回地址，不创建订单      |

**order 对象（可选）：**

| 参数         | 类型   | 必填 | 说明                                                                     |
| ------------ | ------ | ---- | ------------------------------------------------------------------------ |
| orderID      | string | 是   | 商户订单号（传 order 时必填），商户内唯一                                |
| amount       | string | 是   | 期望充值金额（传 order 时必填，支持小数）                                |
| contractAddr | string | 是   | 代币合约地址；原生币写法见 [代币合约地址约定](#代币合约地址约定)         |
| expireAt     | int64  | 否   | 订单过期时间戳（秒）；不传则默认 1小时 过期，过期时间必要是5分钟到24小时 |
| callbackURL  | string | 否   | 订单级充值回调地址；非空时优先于商户默认充值回调地址                     |

请求body示例:
```json
{
    "chain": "tron",
    "accountID": "100200300",
    "order": {
        "amount": "100",
        "orderID": "W20260308700",
        "contractAddr": "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
        "expireAt": 1773150908
    }
}


```

**响应 data：**

| 字段       | 类型   | 说明                                |
| ---------- | ------ | ----------------------------------- |
| addr       | string | 充值地址（与 accountID 绑定，幂等） |
| orderID    | string | 传入 order 时返回                   |
| expireAt   | int64  | 传入 order 时返回订单过期时间戳     |
| paymentUrl | string | 支付页面链接                        |

响应body示例:
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "addr": "TH7tskbKTXVUuerVG97Tj1csBRN3R1buNR",
        "orderID": "W20260308700",
        "expireAt": 1773590073,
        "paymentUrl": "http://154.82.113.141:21001/v2/deposit/pay?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJtaWQiOjIwMDI5MzcxMzU0Njc4NTk5NjgsIm9pZCI6Ilc0MDI2MDMxODgwMzkiLCJleHAiOjE3NzMyNDI2NjUsImlhdCI6MTc3MzIzODY5NX0.7gF-3LHBKeeKWdtW1qdpn3cVDE5hDKDft68OVdK_nfs"
    }
}
```
**说明：** 同一 `chain` + `accountID` 参数相同，返回的充值地址也会相同。accountID最好填用户的ID这样和用户关联的值，这样同一用户，生成的地址永远相同，充值不容易出错。

---

### 充值订单列表

**GET** `/v2/deposit/list`

分页查询本商户的充值订单列表。

**Query 参数：**

| 参数      | 类型   | 必填 | 说明              |
| --------- | ------ | ---- | ----------------- |
| page      | int    | 是   | 页码，从 1 开始   |
| pageSize  | int    | 是   | 每页条数，1～100  |
| accountID | string | 否   | 按 accountID 筛选 |

**响应 data：**

| 字段  | 类型  | 说明         |
| ----- | ----- | ------------ |
| total | int64 | 总条数       |
| items | array | 充值订单列表 |

**items 元素（MerchantDepositOrder）：** 含 `id`、`merchantID`、`orderID`、`chain`、`accountID`、`accountAddr`、`contractAddr`、`amount`、`expireAt`、`status`、`fromTokenValueOnChain`、`createdAt`、`updatedAt` 等。`status`：1=待支付，2=已完成，3=已取消，4=已超时。

响应body示例:
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "total": 2,
        "items": [
            {
                "id": "29",
                "merchantID": "2010528069227384832",
                "orderID": "W302603188007",
                "chain": "eth",
                "accountID": "100200300",
                "accountAddr": "0x3ca5eF10aA02a9bdf3E4F4Bc3370E54489Cc3428",
                "contractAddr": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
                "amount": "13.223",
                "expireAt": 1773150908,
                "status": 2,
                "fromTokenValueOnChain": "82.22",
                "callbackReq": "{\"address\":\"0x3ca5ef10aa02a9bdf3e4f4bc3370e54489cc3428\",\"txid\":\"0x3d3e66d550658dc0b7fb2c5f406f284da79bf47be39ff8caf7cb8c2564d9d77d\",\"time\":1773150528,\"confirmations\":1,\"chain\":\"eth\",\"height\":10421051,\"tokenAddress\":\"0xb65f0057aee4e3d511607a050379b7558a15c67d\",\"tokenSymbol\":\"USDT\",\"tokenValue\":\"13.223\",\"accountID\":\"100200300\",\"orderID\":\"W302603188007\",\"status\":\"success\"}",
                "callbackResp": "ok",
                "callbackURL": "http://127.0.0.1:28080/merchant/testdeposit/create",
                "createdAt": "2026-03-10T21:47:53.285+08:00",
                "updatedAt": "2026-03-10T21:49:18.55+08:00"
            },
            {
                "id": "27",
                "merchantID": "2010528069227384832",
                "orderID": "W302603188006",
                "chain": "eth",
                "accountID": "100200300",
                "accountAddr": "0x3ca5eF10aA02a9bdf3E4F4Bc3370E54489Cc3428",
                "contractAddr": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
                "amount": "13.223",
                "expireAt": 1773150408,
                "status": 4,
                "fromTokenValueOnChain": "",
                "callbackReq": "",
                "callbackResp": "",
                "callbackURL": "http://127.0.0.1:28080/merchant/testdeposit/create",
                "createdAt": "2026-03-10T21:45:25.172+08:00",
                "updatedAt": "2026-03-10T21:46:50.104+08:00"
            }
        ]
    }
}
```

---

### 查询充值订单

**GET** `/v2/deposit/get`

按商户订单号查询一条充值订单。

**Query 参数：**

| 参数    | 类型   | 必填 | 说明                     |
| ------- | ------ | ---- | ------------------------ |
| orderID | string | 是   | 创建充值时传入的 orderID |

**响应 data：** 单条充值订单对象（结构同列表项）。未找到返回「order not found」。

响应body示例:
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "id": "29",
        "merchantID": "2010528069227384832",
        "orderID": "W302603188007",
        "chain": "eth",
        "accountID": "100200300",
        "accountAddr": "0x3ca5eF10aA02a9bdf3E4F4Bc3370E54489Cc3428",
        "contractAddr": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
        "amount": "13.223",
        "expireAt": 1773150908,
        "status": 2,
        "fromTokenValueOnChain": "82.22",
        "callbackReq": "{\"address\":\"0x3ca5ef10aa02a9bdf3e4f4bc3370e54489cc3428\",\"txid\":\"0x3d3e66d550658dc0b7fb2c5f406f284da79bf47be39ff8caf7cb8c2564d9d77d\",\"time\":1773150528,\"confirmations\":1,\"chain\":\"eth\",\"height\":10421051,\"tokenAddress\":\"0xb65f0057aee4e3d511607a050379b7558a15c67d\",\"tokenSymbol\":\"USDT\",\"tokenValue\":\"13.223\",\"accountID\":\"100200300\",\"orderID\":\"W302603188007\",\"status\":\"success\"}",
        "callbackResp": "ok",
        "callbackURL": "W302603188007",
        "createdAt": "2026-03-10T21:47:53.285+08:00",
        "updatedAt": "2026-03-10T21:49:18.55+08:00"
    }
}
```

---

### 取消充值订单

**POST** `/v2/deposit/cancel`

取消一条待支付的充值订单。

**请求体（JSON）：**

| 参数    | 类型   | 必填 | 说明       |
| ------- | ------ | ---- | ---------- |
| orderID | string | 是   | 商户订单号 |

请求body示例
```json
{
    "orderID": "W302603188008"
}
```

响应body示例
```json
{
    "code": 0,
    "msg": "success"
}
```

---

## 地址列表

**GET** `/v2/address/list`

分页查询本商户在指定链上的充值地址列表。

**Query 参数：**

| 参数     | 类型   | 必填 | 说明                       |
| -------- | ------ | ---- | -------------------------- |
| chain    | string | 是   | 链：`eth`、`tron` 或 `ton` |
| page     | int    | 是   | 页码，从 1 开始            |
| pageSize | int    | 是   | 每页条数，1～100           |

**响应 data：**

| 字段  | 类型  | 说明     |
| ----- | ----- | -------- |
| total | int64 | 总条数   |
| items | array | 地址列表 |

**items 元素：** `addr`（地址）、`accountID`（账户 ID）、`createdAt`（创建时间）。

响应body示例
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "total": 2,
        "items": [
            {
                "addr": "TH7tskbKTXVUuerVG97Tj1csBRN3R1buNR",
                "accountID": "100200300",
                "createdAt": "2026-03-08T23:42:17.769+08:00"
            },
            {
                "addr": "TPSTqFbirAzFS3898zx9cUNiehxLe6XBWz",
                "accountID": "2029201139668357120",
                "createdAt": "2026-03-04T22:23:47.091+08:00"
            }
        ]
    }
}
```

---

## 提现（Withdraw）

### 创建提现

**POST** `/v2/withdraw/create`

提交一笔提现申请，平台将异步处理并回调结果。以 **orderID** 作为商户侧唯一标识，同一商户下 orderID 不可重复。

**请求体（JSON）：**

| 参数         | 类型   | 必填 | 说明                                                 |
| ------------ | ------ | ---- | ---------------------------------------------------- |
| chain        | string | 是   | 链：`eth`、`tron` 或 `ton`                           |
| toAddr       | string | 是   | 收款地址（ETH 为 0x；TRON 为 T 开头）                |
| contractAddr | string | 是   | 代币合约地址；原生币写法见 [代币合约地址约定](#代币合约地址约定) |
| amount       | string | 是   | 提现数量（支持小数）                                 |
| orderID      | string | 是   | 商户提现订单号，商户内唯一                           |
| callbackURL  | string | 否   | 订单级提现回调地址；非空时优先于商户默认提现回调地址 |
| accountID    | string | 否   | 系统钱包 accountID（与充值 `accountID` / V1 `addressIdx` 同义）。**仅 Tron 且需写入客户地址监控时传入**；未传或为 0 时不创建提现监控记录（充值产生的监控不受影响） |

请求body示例
```json
{
    "chain": "tron",
    "toAddr": "TCCekL6T3xrpXGPXUMoV1YarknAPLayVxH",
    "contractAddr": "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
    "amount": "12",
    "orderID": "D20260308832",
    "callbackURL": "http://127.0.0.1:28080/merchant/testwithdrawwebhook/create",
    "accountID": "10001"
}
```

**响应 data：**

| 字段    | 类型   | 说明       |
| ------- | ------ | ---------- |
| orderID | string | 与请求一致 |

响应body示例
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "orderID": "D20260308832"
    }
}
```


---

### 提现记录列表

**GET** `/v2/withdraw/list`

分页查询本商户在指定链上的提现记录。

**Query 参数：**

| 参数     | 类型   | 必填 | 说明                       |
| -------- | ------ | ---- | -------------------------- |
| chain    | string | 是   | 链：`eth`、`tron` 或 `ton` |
| page     | int    | 是   | 页码，从 1 开始            |
| pageSize | int    | 是   | 每页条数，1～100           |

**响应 data：**

| 字段  | 类型  | 说明                                                                            |
| ----- | ----- | ------------------------------------------------------------------------------- |
| total | int64 | 总条数                                                                          |
| items | array | 提现记录列表（ETH 为 Erc20Withdraw，TRON 为 Trc20Withdraw，TON 为 TonWithdraw） |

**items 元素：** 含 `id`、`orderID`、`from`、`to`、`txid`、`tokenAddress`、`tokenSymbol`、`amount`、`memo`（V2 中与 orderID 一致）、`status`、`confirmNum`、`currentConfirmNum`、`toTokenValueBeforeWithdraw`、`createdAt`、`updatedAt` 等。`status`：1=已入库等待广播，2=已广播等待确认，3=已完成，4=链上失败，5=已取消，6=未知。

响应json示例
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "total": 1,
        "items": [
            {
                "id": "11",
                "merchantID": "2010528069227384832",
                "from": "0x8BCDdc8f4f981fEE270ebc07840C4d1ed73C50F0",
                "to": "0xB5f5a9aaee4305c6503F91c1247a9ECAE7e09df8",
                "txid": "0xf248e5dbb6324f2d1e1cd1a4df9689124aa9ac69594848ed8544de4f88a41f94",
                "height": 10419412,
                "timestamp": 1773126888,
                "tokenAddress": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
                "tokenSymbol": "USDT",
                "amount": "11",
                "memo": "D20260308821",
                "orderID": "D20260308821",
                "status": 3,
                "confirmNum": 1,
                "currentConfirmNum": 1,
                "toTokenValueBeforeWithdraw": "2.12",
                "callbackURL": "http://127.0.0.1:28080/merchant/testwithdrawwebhook/create2",
                "createdAt": "2026-03-10T15:14:19.272+08:00",
                "updatedAt": "2026-03-10T15:15:02.304+08:00"
            }
        ]
    }
}
```

---

### 查询提现

**GET** `/v2/withdraw/get`

按 orderID 查询一条提现记录。

**Query 参数：**

| 参数    | 类型   | 必填 | 说明                       |
| ------- | ------ | ---- | -------------------------- |
| chain   | string | 是   | 链：`eth`、`tron` 或 `ton` |
| orderID | string | 是   | 创建提现时传入的 orderID   |

**响应 data：** `item` 为单条提现记录对象。

响应json示例
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "item": {
            "id": "11",
            "merchantID": "2010528069227384832",
            "from": "0x8BCDdc8f4f981fEE270ebc07840C4d1ed73C50F0",
            "to": "0xB5f5a9aaee4305c6503F91c1247a9ECAE7e09df8",
            "txid": "0xf248e5dbb6324f2d1e1cd1a4df9689124aa9ac69594848ed8544de4f88a41f94",
            "height": 10419412,
            "timestamp": 1773126888,
            "tokenAddress": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
            "tokenSymbol": "USDT",
            "amount": "11",
            "memo": "D20260308821",
            "orderID": "D20260308821",
            "status": 3,
            "confirmNum": 1,
            "currentConfirmNum": 1,
            "toTokenValueBeforeWithdraw": "2.12",
            "callbackURL": "http://127.0.0.1:28080/merchant/testwithdrawwebhook/create2",
            "createdAt": "2026-03-10T15:14:19.272+08:00",
            "updatedAt": "2026-03-10T15:15:02.304+08:00"
        }
    }
}
```

---

### 取消提现

**POST** `/v2/withdraw/cancel`

取消一笔尚未广播的提现。按 orderID 查找；自动区分 ETH/TRON/TON。

**请求体（JSON）：**

| 参数    | 类型   | 必填 | 说明           |
| ------- | ------ | ---- | -------------- |
| orderID | string | 是   | 商户提现订单号 |

请求body示例
```json
{
    "orderID": "D20260308832"
}
```

响应body示例
```json
{
    "code": 0,
    "msg": "success"
}
```

---

### 查询余额

**GET** `/v2/withdraw/balance`

查询当前商户在指定链、指定代币下的**提现地址**余额（代币余额与 gas 费余额）。用于提现前确认可用额度。默认返回缓存数据；传 `syncFromChain=true` 时从链上拉取最新余额。

**Query 参数：**

| 参数          | 类型   | 必填 | 说明                                                                    |
| ------------- | ------ | ---- | ----------------------------------------------------------------------- |
| chain         | string | 是   | 链：`eth`、`tron` 或 `ton`                                              |
| contractAddr  | string | 是   | 目标代币合约；原生币（含 gas 币种）取值见 [代币合约地址约定](#代币合约地址约定)；|
| syncFromChain | bool   | 否   | 为 `true` 时从链上同步最新余额，再返回；不传或为 `false` 时仅返回缓存值 |

**响应 data：**

| 字段         | 类型   | 说明                                                                       |
| ------------ | ------ | -------------------------------------------------------------------------- |
| tokenBalance | string | 该合约代币余额（可用于提现的额度）                                         |
| gasBalance   | string | 链上 gas 费余额：ETH 为 ETH，TRON 为 TRX，TON 为 TON（用于支付链上手续费） |
| tokenSymbol  | string | 代币符号（如 ETH、TRX、TON、USDT 等），便于前端展示                        |

响应json示例
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "gasBalance": "995.783",
        "tokenBalance": "696.9",
        "tokenSymbol": "USDT"
    }
}
```
--- 

## 回调说明
### 充值回调
| 字段名         | 类型   | 说明                                                    |
| -------------- | ------ | ------------------------------------------------------- |
| accountID      | string | 商户在钱包平台的唯一账号标识                            |
| address        | string | 充值收款地址                                            |
| chain          | string | 区块链名称，如 `eth`、`tron`、`ton`、`btc`                |
| confirmations  | int    | 区块链上确认数（通常 ≥1 表示已到账）                    |
| height         | int    | 区块高度，交易被打包入的区块编号                        |
| orderID        | string | 商户系统生成的充值订单号                                |
| status         | string | 充值状态；见下方说明。ETH / TRON / TON 等链一般为 `success`（到账）、`timeout`、`cancel`；BTC 另见 [BTC 充值 status 说明](#btc-充值-status-说明) |
| time           | int    | 充值成功时间，Unix 时间戳（秒）                         |
| tokenAddress   | string | 充值的代币合约地址                                      |
| tokenSymbol    | string | 充值代币符号，如 `USDT`                                 |
| tokenValue     | string | 充值的代币数量（字符串格式，适配大数）                  |
| fromTokenValue | string | from地址的代币数量                                      |
| txid           | string | 区块链交易哈希                                          |

#### BTC 充值 status 说明

BTC 同一笔 `txid` 可能收到多次回调，通过 **`status`** 区分阶段（不再使用 `op` 字段）。ETH / TRON / TON 等链仍仅在链上确认后回调，且 `status` 一般为 `success`。

| status 值 | 含义 | 典型场景 |
| --------- | ---- | -------- |
| `mempool` | 内存池检测到入账，尚未打包 | 开启内存池监控时，交易出现在内存池中 |
| `packed` | 已打包进块，尚未达到商户配置的确认数 | 扫块发现入账，或内存池交易被打包 |
| `success` | 已达到确认数，入账完成 | 与 ETH/TRON 等链「到账成功」回调一致 |
| `cancel` | 入账取消或无法完成确认 | 确认时库中已无对应充值记录；或内存池交易被替换/驱逐且未上链等 |

**接入建议：** 按 `txid` + `address` 幂等处理；`mempool` / `packed` 仅作进度通知，资金入账以 `success` 为准。

* BTC 内存池示例（`status=mempool`）

```json
{
  "accountID": "100200300",
  "address": "tb1qxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "chain": "btc",
  "confirmations": 0,
  "height": 0,
  "status": "mempool",
  "time": 1773150528,
  "tokenAddress": "__BTC_NATIVE__",
  "tokenSymbol": "BTC",
  "tokenValue": "0.001",
  "txid": "abc123..."
}
```

* BTC 已打包（未确认）示例（`status=packed`）

```json
{
  "accountID": "100200300",
  "address": "tb1qxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "chain": "btc",
  "confirmations": 0,
  "height": 920000,
  "status": "packed",
  "time": 1773150528,
  "tokenAddress": "__BTC_NATIVE__",
  "tokenSymbol": "BTC",
  "tokenValue": "0.001",
  "txid": "abc123..."
}
```

* BTC 确认到账示例（`status=success`）

```json
{
  "accountID": "100200300",
  "address": "tb1qxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "chain": "btc",
  "confirmations": 1,
  "height": 920000,
  "status": "success",
  "time": 1773150528,
  "tokenAddress": "__BTC_NATIVE__",
  "tokenSymbol": "BTC",
  "tokenValue": "0.001",
  "txid": "abc123..."
}
```

* BTC 取消示例（`status=cancel`）

```json
{
  "accountID": "100200300",
  "address": "tb1qxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "chain": "btc",
  "confirmations": 0,
  "height": 0,
  "status": "cancel",
  "time": 1773150528,
  "tokenAddress": "__BTC_NATIVE__",
  "tokenSymbol": "BTC",
  "tokenValue": "0.001",
  "txid": "abc123..."
}
```

* 正常回调示例
```json
{
    "accountID": "100200300",
    "address": "0x3ca5ef10aa02a9bdf3e4f4bc3370e54489cc3428",
    "chain": "eth",
    "confirmations": 1,
    "height": 10421051,
    "orderID": "W302603188007",
    "status": "success",
    "time": 1773150528,
    "tokenAddress": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
    "tokenSymbol": "USDT",
    "tokenValue": "13.223",
    "fromTokenValue": "82.22",
    "txid": "0x3d3e66d550658dc0b7fb2c5f406f284da79bf47be39ff8caf7cb8c2564d9d77d"
  }
```
* 失败回调示例
  * 超时
    ```json
    {
        "accountID": "100200300",
        "address": "0x3ca5eF10aA02a9bdf3E4F4Bc3370E54489Cc3428",
        "chain": "eth",
        "confirmations": 0,
        "height": 0,
        "orderID": "W302603188006",
        "status": "timeout",
        "time": 0,
        "tokenAddress": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
        "tokenSymbol": "",
        "tokenValue": "13.223",
        "fromTokenValue": "82.22",
        "txid": ""
    }
    ```
  * 取消
    ```json
    {
        "accountID": "100200300",
        "address": "0x3ca5eF10aA02a9bdf3E4F4Bc3370E54489Cc3428",
        "chain": "eth",
        "confirmations": 0,
        "height": 0,
        "orderID": "W302603188008",
        "status": "cancel",
        "time": 0,
        "tokenAddress": "0xb65f0057aee4e3d511607a050379b7558a15c67d",
        "tokenSymbol": "",
        "tokenValue": "10",
        "fromTokenValue": "",
        "txid": ""
    }
    ```
### 提现回调
| 字段名                     | 类型   | 说明                                                                                                                   |
| -------------------------- | ------ | ---------------------------------------------------------------------------------------------------------------------- |
| address                    | string | 提现地址                                                                                                               |
| chain                      | string | 区块链名称，如 `eth`、`tron`、`ton`                                                                                    |
| confirmations              | int    | 区块链上确认数（通常 ≥1 表示已到账）                                                                                   |
| height                     | int    | 区块高度，交易被打包入的区块编号                                                                                       |
| memo                       | string | 提现备注/业务方订单号（和 orderID 相同,兼容v1）                                                                        |
| orderID                    | string | 商户系统生成的提现订单号                                                                                               |
| status                     | string | 提现状态；`success`=成功， `cancel`=取消，`token_insufficient`=代币不够，`gas_insufficient`=gas费用不够, `failed`=未知 |
| time                       | int    | 提现完成时间，Unix 时间戳（秒）                                                                                        |
| tokenAddress               | string | 提现代币合约地址                                                                                                       |
| tokenSymbol                | string | 提现代币符号，如 `USDT`                                                                                                |
| tokenValue                 | string | 提现的代币数量（字符串格式，适配大数）                                                                                 |
| toTokenValueBeforeWithdraw | string | 提现前，收款地址的代币数量（字符串格式，适配大数）                                                                     |
| txid                       | string | 区块链交易哈希                                                                                                         |


* 正常回调示例
```json
{
    "address": "TCCekL6T3xrpXGPXUMoV1YarknAPLayVxH",
    "chain": "tron",
    "confirmations": 1,
    "height": 65562218,
    "memo": "D20260308832",
    "orderID": "D20260308832",
    "status": "success",
    "time": 1773154821,
    "tokenAddress": "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
    "tokenSymbol": "USDT",
    "tokenValue": "12",
    "toTokenValueBeforeWithdraw": "2.12",
    "txid": "49da9fc64f447f1ad162f9cef536ffbdd76ee7db3806a17545455148f7590987"
}
```

### 回调客户返回值

以上 [充值回调](#充值回调)、[提现回调](#提现回调) 收到通知后，响应码需是 **200**，并且应答 body 里必须返回 **`ok`**。如不是正确响应，服务器会自动重试 3 次回调。

---

## 客户地址监控（Tron）

> 仅 Tron 链支持。需在后台为对应 TRC20/TRX 配置开启客户地址监控，并配置**客户地址监控回调 URL**（与充值/提现回调 URL 独立）。

**功能说明**

- **充值监控**：客户向系统充值地址转账，系统将客户的付款地址（`from`）标记为**充值地址**并写入监控。此后该地址出现在任意交易中（无论收款还是付款），均立即推送监控回调。
- **提现监控**：系统向客户地址打款并链上确认后，若创建提现时在请求中传入了 **accountID**（V1 为 **addressIdx**），才会将客户收款地址（`to`）写入监控；**未传则不监控**。此后该地址出现在任意交易中，均立即推送监控回调。
- 监控记录按 **systemDepositAddr**（`accountID` 在 `tron_addrs` 中对应的系统充值地址）区分：充值监控在客户向该地址转账时建立；提现监控在创建提现时传入 `accountID` 后，以同一 `accountID` 的充值地址作为关联键。同一客户地址若分别关联不同 `accountID`，将产生**多条**监控记录。

### 查询监控列表

**GET** `/v2/customer-addr-monitor/list`

按 accountID 查询该钱包下客户地址的监控变化（余额、最近交易等）。目前仅支持 `chain=tron`，不传时默认为 `tron`。

| 参数         | 类型   | 必填 | 说明                                   |
| ------------ | ------ | ---- | -------------------------------------- |
| chain        | string | 否   | 链标识，目前仅支持 `tron`；不传默认为 `tron` |
| accountID    | string | 是   | 系统钱包 accountID（数字或 hash 字符串） |
| contractAddr | string | 否   | 过滤合约地址                           |

响应 `data`：`total` 为条数，`items` 为 `TronCustomerAddrMonitor` 数组（按 `updatedAt` 降序），含 `accountID`、`addr`、`balance`、`lastTxID`、`isDepositAddr`、`isWithdrawAddr`、`systemDepositAddr`、`updatedAt` 等。

响应示例：

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "total": 1,
        "items": [
            {
                "id": "5",
                "merchantID": "2050166292215762944",
                "accountID": "100200300",
                "contractAddr": "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
                "addr": "TCCekL6T3xrpXGPXUMoV1YarknAPLayVxH",
                "isDepositAddr": true,
                "isWithdrawAddr": false,
                "balance": "9919.436",
                "lastTxID": "f7c4a3c9bee91a768117795b12f20b443a5841505b2a16b8e89d02d2b4076da7",
                "systemDepositAddr": "TAMAM25rkuuZffxyAvEwnvHF1jwoZwAKxg",
                "expireAt": "2026-09-26T07:54:25.695+08:00",
                "createdAt": "2026-06-28T07:54:25.505+08:00",
                "updatedAt": "2026-06-28T07:54:25.698+08:00"
            }
        ]
    }
}
```

### 监控回调

当监控中的客户地址出现在任意交易的 from 或 to 时，平台以 **POST** 方式向**客户地址监控回调 URL** 推送 JSON，签名方式与充值/提现回调一致（参见[签名校验](#签名校验)）。

**回调数据格式**

```json
{
    "chain": "tron",
    "contractAddr": "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
    "symbol": "USDT",
    "addr": "TExampleCustomerAddress1234567890AB",
    "balance": "123.456789",
    "relations": [
        {
            "addressIdx": "10001",
            "systemDepositAddr": "THotYegdHdngfJBRMpzTiT8J8yV4XhBBfo",
            "isDepositAddr": true,
            "isWithdrawAddr": false
        },
        {
            "addressIdx": "10002",
            "systemDepositAddr": "TAnotherSystemDepositAddr123456789012",
            "isDepositAddr": false,
            "isWithdrawAddr": true
        }
    ],
    "detail": {
        "txid": "336fc08775278c406ccd1506865d4ac4f1b20c44d7c0a349c4bced36a519ecd9",
        "height": "53310985",
        "balanceChanged": "decrease"
    }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| chain | string | 链标识，固定为 `tron` |
| contractAddr | string | 代币合约地址；TRX（原生代币）为空字符串 `""` |
| symbol | string | 代币符号，如 `USDT`、`TRX` |
| addr | string | 被监控的客户地址 |
| balance | string | 触发时该客户地址的链上代币余额 |
| relations | array | 与系统钱包的关联列表，每项对应一条监控记录 |
| relations[].addressIdx | string | 系统钱包 accountID |
| relations[].systemDepositAddr | string | `accountID` 对应的系统充值地址（`tron_addrs.addr`） |
| relations[].isDepositAddr | bool | 是否因向该 `systemDepositAddr` 充值而监控 |
| relations[].isWithdrawAddr | bool | 是否因创建提现时传入对应 `accountID` 而监控 |
| detail.balanceChanged | string | 余额变化方向：`increase`（收到资金）、`decrease`（转出资金）或 `manual`（手动触发同步） |
| detail.txid | string | 触发本次回调的交易哈希；手动触发时省略 |
| detail.height | string | 触发本次回调的区块高度；手动触发时省略 |

**回调响应要求**：HTTP **200**，body 返回 **`ok`**（与充值/提现回调一致；监控回调失败时平台仅记录日志，不会自动重试）。

---

## Telegram 通知接入

平台支持将商户相关通知（如提现资金不足、回调接口异常等）通过 Telegram 机器人推送到指定群组。接入前需要先创建 Bot、创建或选定通知群，并在后台配置 **Bot Token** 与 **群 Chat ID**。
![telegrambot配置](./tgbot.jpg)

### 获取 Bot Token

1. 在 Telegram 中搜索 **@BotFather**，打开官方机器人。
2. 发送 `/newbot`，按提示设置机器人名称（如「钱包通知助手」）和用户名（必须以 `bot` 结尾，如 `my_wallet_notify_bot`）。
3. 创建成功后，BotFather 会发来一串形如 `1234567890:ABCdefGHI...` 的 **Bot Token**。请妥善保存，后续在后台「商户管理」或「通知配置」中填入 **Bot Token** 一栏。

**注意：** Bot Token 相当于机器人密钥，不要泄露给他人或提交到公开仓库。

### 获取通知群的 Chat ID

1. 在 Telegram 中创建一个**群组**（或使用已有群组），用作接收通知。
2. 将你在上一步创建的 **Bot** 邀请进该群（群设置 → 添加成员 → 搜索 Bot 用户名并添加）。
3. 邀请 @chatIDrobot 进群。这个机器人会输出群的chat_id。
4. 群的 **Chat ID**（例如 `-1001234567890`）。复制该数值，在后台配置时填入 **Chat ID** 配置项。

---

## 错误码说明

接口失败时，`code` 为非 0，`msg` 为具体描述。以下为 API 可能返回的错误码（V1/V2 通用）。

| code  | 说明                                     |
| ----- | ---------------------------------------- |
| 0     | 成功                                     |
| 10000 | 服务器内部错误                           |
| 10001 | 参数错误（必填项、格式、业务校验等）     |
| 10002 | 没有权限                                 |
| 10003 | 重复的 key（如 orderID 已存在）          |
| 10004 | 记录不存在                               |
| 10005 | 第三方服务错误                           |
| 14001 | API Key 错误（缺失、无效或与商户不匹配） |
| 14002 | IP 不在白名单                            |
| 14003 | 商户数量或资源达到上限                   |
| 14004 | 签名不合法                               |
| 20101 | 提现配置错误（未配置、已禁用等）         |
| 20102 | 提现地址 gas 费不足（ETH/TRX/TON 等）    |
| 20103 | 提现地址代币余额不足                     |

详细文案以响应中的 `msg` 为准。

## 测试代币领取说明

平台目前支持 **ETH**、**TRON** 与 **TON** 公链的代币充值/提现（TON 含原生 TON 与 Jetton）。测试环境可通过以下方式获取测试代币：

| 链 / 环境                | 代币 | 合约地址 / 说明                              | 领取方式                                                                 |
| ------------------------ | ---- | -------------------------------------------- | ------------------------------------------------------------------------ |
| **TRON（尼罗河测试网）** | USDT | `TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf`         | 在 [Nile 测试网水龙头](https://nileex.io/join/getJoinPage) 领取测试 USDT |
| **ETH（测试网）**        | USDT | `0xb65F0057AEE4e3D511607a050379B7558a15c67D` | 该合约为平台提供的测试 USDT，需向平台申请领取                            |

---

# 数字钱包平台 - 第三方接入文档（V2 API）

本文档面向接入方，描述如何通过 **V2 API** 与数字钱包平台集成，包括鉴权方式、接口列表、请求与响应格式

- [数字钱包平台 - 第三方接入文档（V2 API）](#数字钱包平台---第三方接入文档v2-api)
  - [接入流程建议](#接入流程建议)
  - [鉴权说明](#鉴权说明)
    - [API Key](#api-key)
    - [签名校验](#签名校验)
  - [V2 接口概览](#v2-接口概览)
    - [通用响应结构](#通用响应结构)
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
    - [提现回调](#提现回调)
    - [回调客户返回值](#回调客户返回值)
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

### 通用响应结构

接口统一返回 JSON，所有响应均包含 `code` 与 `msg`；成功时另有 `data` 承载业务数据。

**成功：** `{ "code": 0, "msg": "success", "data": { ... } }`  
**失败：** `{ "code": 10001, "msg": "错误描述" }`

- `code`：0 表示成功，非 0 见 [错误码说明](#错误码说明)。
- `msg`：成功时为 `"success"`，失败时为具体描述。
- `data`：成功时按接口约定返回；失败时通常无或为空。

---

## 充值（Deposit）

### 创建充值 / 获取地址

**POST** `/v2/deposit/create`

获取或创建一条与 `accountID` 绑定的充值地址；可选传入 `order` 创建充值订单（金额、orderID、过期时间等），便于后续按订单查询与回调关联。

**请求体（JSON）：**

| 参数      | 类型   | 必填 | 说明                                        |
| --------- | ------ | ---- | ------------------------------------------- |
| chain     | string | 是   | 链：`eth` 或 `tron`                         |
| accountID | string | 是   | 账户ID。相同的accountID生成的地址会是一样的 |
| order     | object | 否   | 订单信息；不传则仅返回地址，不创建订单      |

**order 对象（可选）：**

| 参数         | 类型   | 必填 | 说明                                                 |
| ------------ | ------ | ---- | ---------------------------------------------------- |
| orderID      | string | 是  | 商户订单号（传 order 时必填），商户内唯一            |
| amount       | string | 是  | 期望充值金额（传 order 时必填，支持小数）            |
| contractAddr | string | 是   | 代币合约地址                                         |
| expireAt     | int64  | 否   | 订单过期时间戳（秒）；不传则默认 7 天内              |
| callbackURL  | string | 否   | 订单级充值回调地址；非空时优先于商户默认充值回调地址 |

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

| 字段     | 类型   | 说明                                |
| -------- | ------ | ----------------------------------- |
| addr     | string | 充值地址（与 accountID 绑定，幂等） |
| orderID  | string | 传入 order 时返回                   |
| expireAt | int64  | 传入 order 时返回订单过期时间戳     |

响应body示例:
```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "addr": "TH7tskbKTXVUuerVG97Tj1csBRN3R1buNR",
        "orderID": "W20260308700",
        "expireAt": 1773590073
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

**items 元素（MerchantDepositOrder）：** 含 `id`、`merchantID`、`orderID`、`chain`、`accountID`、`accountAddr`、`contractAddr`、`amount`、`expireAt`、`status`、`createdAt`、`updatedAt` 等。`status`：1=待支付，2=已完成，3=已取消，4=已超时。

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

| 参数     | 类型   | 必填 | 说明                |
| -------- | ------ | ---- | ------------------- |
| chain    | string | 是   | 链：`eth` 或 `tron` |
| page     | int    | 是   | 页码，从 1 开始     |
| pageSize | int    | 是   | 每页条数，1～100    |

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
| chain        | string | 是   | 链：`eth` 或 `tron`                                  |
| toAddr       | string | 是   | 收款地址（ETH 为 0x 格式，TRON 为 T 开头）           |
| contractAddr | string | 是   | 代币合约地址                                         |
| amount       | string | 是   | 提现数量（支持小数）                                 |
| orderID      | string | 是   | 商户提现订单号，商户内唯一                           |
| callbackURL  | string | 否   | 订单级提现回调地址；非空时优先于商户默认提现回调地址 |

请求body示例
```json
{
    "chain": "tron",
    "toAddr": "TCCekL6T3xrpXGPXUMoV1YarknAPLayVxH",
    "contractAddr": "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
    "amount": "12",
    "orderID": "D20260308832",
    "callbackURL": "http://127.0.0.1:28080/merchant/testwithdrawwebhook/create"
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

| 参数     | 类型   | 必填 | 说明                |
| -------- | ------ | ---- | ------------------- |
| chain    | string | 是   | 链：`eth` 或 `tron` |
| page     | int    | 是   | 页码，从 1 开始     |
| pageSize | int    | 是   | 每页条数，1～100    |

**响应 data：**

| 字段  | 类型  | 说明                                                        |
| ----- | ----- | ----------------------------------------------------------- |
| total | int64 | 总条数                                                      |
| items | array | 提现记录列表（ETH 为 Erc20Withdraw，TRON 为 Trc20Withdraw） |

**items 元素：** 含 `id`、`orderID`、`from`、`to`、`txid`、`tokenAddress`、`tokenSymbol`、`amount`、`memo`（V2 中与 orderID 一致）、`status`、`confirmNum`、`currentConfirmNum`、`createdAt`、`updatedAt` 等。`status`：1=已入库等待广播，2=已广播等待确认，3=已完成，4=链上失败，5=已取消，6=未知。

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

| 参数    | 类型   | 必填 | 说明                     |
| ------- | ------ | ---- | ------------------------ |
| chain   | string | 是   | 链：`eth` 或 `tron`      |
| orderID | string | 是   | 创建提现时传入的 orderID |

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

取消一笔尚未广播的提现。按 orderID 查找，自动区分 ETH/TRON。

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

| 参数          | 类型   | 必填 | 说明                                                                                                 |
| ------------- | ------ | ---- | ---------------------------------------------------------------------------------------------------- |
| chain         | string | 是   | 链：`eth` 或 `tron`                                                                                  |
| contractAddr  | string | 是   | 代币合约：ETH 链传空或 `eth` 表示原生 ETH；TRON 链传空或 `trx` 表示 TRX，否则为 TRC20/ERC20 合约地址 |
| syncFromChain | bool   | 否   | 为 `true` 时从链上同步最新余额，再返回；不传或为 `false` 时仅返回缓存值                              |

**响应 data：**

| 字段         | 类型   | 说明                                                   |
| ------------ | ------ | ------------------------------------------------------ |
| tokenBalance | string | 该合约代币余额（可用于提现的额度）                     |
| gasBalance   | string | 链上 gas 费余额：ETH 链为 ETH 余额，TRON 链为 TRX 余额 |
| tokenSymbol  | string | 代币符号（如 ETH、TRX、USDT），便于前端展示            |

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
| 字段名        | 类型   | 说明                                                    |
| ------------- | ------ | ------------------------------------------------------- |
| accountID     | string | 商户在钱包平台的唯一账号标识                            |
| address       | string | 充值收款地址                                            |
| chain         | string | 区块链名称，如 `eth`  `tron`                            |
| confirmations | int    | 区块链上确认数（通常 ≥1 表示已到账）                    |
| height        | int    | 区块高度，交易被打包入的区块编号                        |
| orderID       | string | 商户系统生成的充值订单号                                |
| status        | string | 充值状态；`success`=成功，`timeout`=超时，`cancel`=取消 |
| time          | int    | 充值成功时间，Unix 时间戳（秒）                         |
| tokenAddress  | string | 充值的代币合约地址                                      |
| tokenSymbol   | string | 充值代币符号，如 `USDT`                                 |
| tokenValue    | string | 充值的代币数量（字符串格式，适配大数）                  |
| txid          | string | 区块链交易哈希                                          |

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
        "txid": ""
    }
    ```
### 提现回调
| 字段名        | 类型   | 说明                                                    |
| ------------- | ------ | ------------------------------------------------------- |
| address       | string | 提现地址                                                |
| chain         | string | 区块链名称，如 `eth`  `tron`                            |
| confirmations | int    | 区块链上确认数（通常 ≥1 表示已到账）                    |
| height        | int    | 区块高度，交易被打包入的区块编号                        |
| memo          | string | 提现备注/业务方订单号（和 orderID 相同,兼容v1）                |
| orderID       | string | 商户系统生成的提现订单号                                |
| status        | string | 提现状态；`success`=成功，`token_insufficient`=代币不够，`gas_insufficient`=gas费用不够 |
| time          | int    | 提现完成时间，Unix 时间戳（秒）                         |
| tokenAddress  | string | 提现代币合约地址                                        |
| tokenSymbol   | string | 提现代币符号，如 `USDT`                                 |
| tokenValue    | string | 提现的代币数量（字符串格式，适配大数）                  |
| txid          | string | 区块链交易哈希                                          |


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
    "txid": "49da9fc64f447f1ad162f9cef536ffbdd76ee7db3806a17545455148f7590987"
}
```

### 回调客户返回值
客户收到通知后，响应码需是200，并且应答body里必须返回ok。如不是正确响应，服务器会自动重试3次回调。

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
| 20102 | 提现地址 gas 费不足（TRX/ETH）           |
| 20103 | 提现地址代币余额不足                     |

详细文案以响应中的 `msg` 为准。

## 测试代币领取说明

平台目前支持 **ETH** 和 **TRON** 公链的 USDT 等代币充值/提现，测试环境可通过以下方式获取测试代币：

| 链 / 环境                | 代币 | 合约地址 / 说明                              | 领取方式                                                                 |
| ------------------------ | ---- | -------------------------------------------- | ------------------------------------------------------------------------ |
| **TRON（尼罗河测试网）** | USDT | `TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf`         | 在 [Nile 测试网水龙头](https://nileex.io/join/getJoinPage) 领取测试 USDT |
| **ETH（测试网）**        | USDT | `0xb65F0057AEE4e3D511607a050379B7558a15c67D` | 该合约为平台提供的测试 USDT，需向平台申请领取                            |

---

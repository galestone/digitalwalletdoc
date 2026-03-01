# 数字钱包平台 - 第三方接入文档

本文档面向接入方，描述如何通过 API 与数字钱包平台集成，包括鉴权方式、接口列表、请求与响应格式。

- [数字钱包平台 - 第三方接入文档](#数字钱包平台---第三方接入文档)
  - [一、接入流程建议](#一接入流程建议)
  - [二、鉴权说明](#二鉴权说明)
    - [API Key](#api-key)
    - [通用响应结构](#通用响应结构)
  - [三、TRON 相关接口](#三tron-相关接口)
    - [地址管理](#地址管理)
      - [生成地址](#生成地址)
      - [地址列表（分页）](#地址列表分页)
      - [按索引查询地址](#按索引查询地址)
    - [提现](#提现)
      - [申请提现](#申请提现)
      - [链上取消提现](#链上取消提现)
      - [提现记录列表（分页）](#提现记录列表分页)
  - [四、ETH 相关接口](#四eth-相关接口)
    - [地址管理](#地址管理-1)
      - [生成地址](#生成地址-1)
      - [地址列表（分页）](#地址列表分页-1)
      - [按索引查询地址](#按索引查询地址-1)
  - [五、回调历史查询](#五回调历史查询)
    - [充值回调历史](#充值回调历史)
    - [提现回调历史](#提现回调历史)
  - [六、平台主动回调（Webhook）](#六平台主动回调webhook)
    - [回调签名与认证](#回调签名与认证)
    - [验证收到的签名](#验证收到的签名)

---

## 一、接入流程建议

1. 平台会给后台用户，[点我登录测试后台](http://154.82.113.141:12583/login)，登录后台创建商户（可能平台给账户前已经创建了），配置充值/提现回调 URL，添加后会生成 **API Key** 和 **Signature Key**。
![新增商户](./新增商户.png)
2. 拿到 **API Key** 后，就可以调用api接口了。下面开始对接充值和提现流程
3. 充值流程
   1. 为用户生成充值地址。 以TRX为例，调用[生成地址接口](#生成地址)可生成。
   2. 用户拿到地址后，可以往地址中充值。
   3. 钱包系统监测到充值时，会调用充值回调
4. 提现流程
   1. 调用提现接口
   2. 链上确认后，钱包会调用回调
5. 注：平台目前仅支持地eth和tron公链的usdt充值，可以通过如下方式获取测试代币。
   1. 波长测试网的usdt是TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf。可在https://nileex.io/join/getJoinPage领取测试。
   2. eth的测试的usdt是0xb65F0057AEE4e3D511607a050379B7558a15c67D。这个平台发的测试币，需要找平台要。

---

## 二、鉴权说明

### API Key

所有接口均需在请求头中携带商户 API Key：

| Header 名称 | 说明 |
|------------|------|
| `apikey`   | 在后台「商户管理」中获取，用于标识商户身份 |

示例：

```
apikey: your_merchant_api_key_here
```

### 通用响应结构

接口统一返回 JSON，结构如下：

**成功时：**

```json
{
  "data": { ... }
}
```

**失败时：**

```json
{
  "code": 40001,
  "msg": "错误描述信息"
}
```

- `code`：错误码，仅失败时存在  
- `msg`：错误说明  
- `data`：业务数据，仅成功时存在  

---

## 三、TRON 相关接口

基础路径：`/v1/tron`

### 地址管理

#### 生成地址

**POST** `/v1/tron/address/gen`

生成一个归属于当前商户的 TRON 充值地址。

**请求体（JSON）：**

| 参数         | 类型 | 必填 | 说明 |
|--------------|------|------|------|
| addressIdx   | int64 | 否  | 自定义地址索引；不传或为 0 时由系统生成 |

**响应 data：**

| 字段       | 类型  | 说明 |
|------------|-------|------|
| addr       | string | 生成的 TRON 地址 |
| addressIdx | int64  | 地址索引（请求传入或系统生成） |

**说明：** 传入已存在的 `addressIdx`，会返回该索引已绑定的地址（幂等）。建议传入用户id。

---

#### 地址列表（分页）

**GET** `/v1/tron/address/list`

分页查询本商户的 TRON 地址列表。

**Query 参数：**

| 参数     | 类型 | 必填 | 说明 |
|----------|------|------|------|
| page     | int  | 是   | 页码，从 1 开始 |
| pageSize | int  | 是   | 每页条数，范围 1～100 |
| addr     | string | 否 | 按地址模糊筛选 |

**响应 data：**

| 字段  | 类型   | 说明 |
|-------|--------|------|
| total | int64  | 总条数 |
| items | array  | 当前页地址列表 |

**items 元素：**

| 字段      | 类型   | 说明 |
|-----------|--------|------|
| addr      | string | 地址 |
| accountID | int64  | 地址索引 |
| createdAt | string | 创建时间（ISO 格式） |

---

#### 按索引查询地址

**GET** `/v1/tron/address/get`

根据地址索引查询本商户下的 TRON 地址。

**Query 参数：**

| 参数      | 类型 | 必填 | 说明     |
|-----------|------|------|----------|
| accountID | int64 | 是  | 地址索引 |

**响应 data：**

| 字段 | 类型   | 说明 |
|------|--------|------|
| addr | string | 对应的 TRON 地址 |

---

### 提现

#### 申请提现

**POST** `/v1/tron/withdraw/apply`

提交一笔 TRX 或 TRC20 提现申请，平台将异步处理并回调结果。

**请求体（JSON）：**

| 参数          | 类型   | 必填 | 说明 |
|---------------|--------|------|------|
| toAddr        | string | 是   | 收款方 TRON 地址 |
| amount        | string | 是   | 提现数量（支持小数，如 "100.5"） |
| contractAddr  | string | 是   | 代币合约地址：提 TRX 时传 `T000000000000000000000000000000000`；提 TRC20 时传对应合约地址 |
| memo          | string | 否   | 备注，会随提现回调带给接入方 |

**响应 data：**

| 字段 | 类型   | 说明 |
|------|--------|------|
| id   | string | 提现记录 ID，用于 [链上取消提现](#链上取消提现) |

成功仅表示已入库，实际到账以回调为准。

**说明：**

- 提现前需在后台为该商户、该合约配置提现参数（最小/最大金额、确认数等），否则会报「withdraw config not found」或「withdraw config disabled」。
- `amount` 需大于 0，且不超过后台配置的 `maxWithdrawAmount`（若已配置）。

---

#### 链上取消提现

**POST** `/v1/tron/withdraw/cancel`

取消一笔尚未广播到链上的提现申请。仅当提现状态为「已入库等待广播」时可取消；一旦已广播则不可取消。

**请求体（JSON）：**

| 参数 | 类型   | 必填 | 说明 |
|------|--------|------|------|
| id   | string | 是   | 提现记录 ID（[申请提现](#申请提现) 接口返回的 `id`） |

**响应 data：** 空对象 `{}`。成功表示已取消，该笔提现不会上链。

---

#### 提现记录列表（分页）

**GET** `/v1/tron/withdraw/list`

分页查询本商户的 TRON/TRC20 提现记录。

**Query 参数：**

| 参数     | 类型   | 必填 | 说明 |
|----------|--------|------|------|
| page     | int    | 是   | 页码，从 1 开始 |
| pageSize | int    | 是   | 每页条数，范围 1～100 |
| addr     | string | 否   | 按地址筛选（from/to） |

**响应 data：**

| 字段  | 类型   | 说明 |
|-------|--------|------|
| total | int64  | 总条数 |
| items | array  | 提现记录列表 |

**items 元素（提现记录）：**

| 字段                | 类型   | 说明 |
|---------------------|--------|------|
| id                  | string | 记录 ID |
| merchantID          | string | 商户 ID |
| from                | string | 转出地址 |
| to                  | string | 收款地址 |
| txid                | string | 交易哈希（已广播后有值） |
| tokenAddress        | string | 代币合约地址（TRX 时为固定常量） |
| tokenSymbol         | string | 代币符号，如 TRX、USDT |
| amount              | string | 提现数量 |
| memo                | string | 备注 |
| status              | int    | 状态：1=已入库等待广播，2=已广播等待确认，3=已完成，4=链上失败，5=已取消，6=未知 |
| confirmNum          | int64  | 所需确认数 |
| currentConfirmNum   | int64  | 当前确认数 |
| createdAt / updatedAt | string | 创建/更新时间 |

---

## 四、ETH 相关接口

基础路径：`/v1/eth`

### 地址管理

#### 生成地址

**POST** `/v1/eth/address/gen`

生成一个归属于当前商户的 ETH 充值地址。

**请求体（JSON）：**

| 参数       | 类型  | 必填 | 说明 |
|------------|-------|------|------|
| addressIdx | int64 | 否   | 自定义地址索引；不传或为 0 时由系统生成 |

**响应 data：**

| 字段       | 类型   | 说明 |
|------------|--------|------|
| addr       | string | 生成的 ETH 地址 |
| addressIdx | int64  | 地址索引 |

规则与 TRON 类似：存在单商户地址数量上限；重复 `addressIdx` 时返回已绑定地址（幂等）。

---

#### 地址列表（分页）

**GET** `/v1/eth/address/list`

分页查询本商户的 ETH 地址列表。

**Query 参数：**

| 参数     | 类型   | 必填 | 说明 |
|----------|--------|------|------|
| page     | int    | 是   | 页码，从 1 开始 |
| pageSize | int    | 是   | 每页条数，1～100 |
| addr     | string | 否   | 按地址模糊筛选 |

**响应 data：**

| 字段  | 类型   | 说明 |
|-------|--------|------|
| total | int64  | 总条数 |
| items | array  | 当前页地址列表 |

**items 元素：**

| 字段      | 类型   | 说明 |
|-----------|--------|------|
| addr      | string | 地址 |
| accountID | int64  | 地址索引 |
| createdAt | string | 创建时间 |

---

#### 按索引查询地址

**GET** `/v1/eth/address/get`

根据地址索引查询本商户的 ETH 地址。

**Query 参数：**

| 参数      | 类型 | 必填 | 说明     |
|-----------|------|------|----------|
| accountID | int64 | 是  | 地址索引 |

**响应 data：**

| 字段 | 类型   | 说明 |
|------|--------|------|
| addr | string | 对应的 ETH 地址 |

---

## 五、回调历史查询

### 充值回调历史

**GET** `/v1/list`

分页查询本商户的**充值** Webhook 回调历史（即平台向接入方推送充值通知的记录）。

**Query 参数：**

| 参数     | 类型   | 必填 | 说明 |
|----------|--------|------|------|
| page     | int    | 是   | 页码，从 1 开始 |
| pageSize | int    | 是   | 每页条数，1～100 |
| address  | string | 否   | 按充值地址筛选 |
| txid     | string | 否   | 按交易哈希筛选 |

**响应 data：**

| 字段  | 类型   | 说明 |
|-------|--------|------|
| total | int64  | 总条数 |
| items | array  | 回调历史记录 |

**items 元素：**

| 字段             | 类型   | 说明 |
|------------------|--------|------|
| id               | string | 记录 ID |
| merchantID       | string | 商户 ID |
| address          | string | 充值到的地址 |
| txid             | string | 交易哈希 |
| chain            | string | 链标识，如 tron |
| tokenAddress     | string | 代币合约地址 |
| tokenSymbol      | string | 代币符号 |
| tokenValue       | string | 充值数量 |
| url              | string | 回调 URL |
| httpStatusCode   | int    | 接入方返回的 HTTP 状态码 |
| callbackSuccess  | int    | 回调状态：1=成功，2=重试中，3=重试后失败，4=已手动处理 |
| tryTimes         | int    | 已重试次数 |
| createdAt        | string | 创建时间 |

---

### 提现回调历史

**GET** `/v1/withdraw/list`

分页查询本商户的**提现** Webhook 回调历史（即平台向接入方推送提现结果的通知记录）。

**Query 参数：**

| 参数     | 类型   | 必填 | 说明 |
|----------|--------|------|------|
| page     | int    | 是   | 页码，从 1 开始 |
| pageSize | int    | 是   | 每页条数，1～100 |
| address  | string | 否   | 按地址筛选 |
| txid     | string | 否   | 按交易哈希筛选 |

**响应 data：**

| 字段  | 类型   | 说明 |
|-------|--------|------|
| total | int64  | 总条数 |
| items | array  | 提现回调历史记录 |

**items 元素：** 与充值回调类似，并包含 `memo`、`from`、`withdrawInfo` 等提现相关字段，以及 `isFinalConfirm`（是否已达最终确认数）等。

---

## 六、平台主动回调（Webhook）

平台会向商户在后台配置的 URL 主动推送**充值**与**提现**结果，接入方需实现可公网访问的 HTTP 接口并验证签名。

### 回调签名与认证

为确保 Webhook 安全，请使用您在「数字钱包」后台配置的**签名密钥（signing key）**生成的 HMAC SHA-256 哈希来验证请求是否来自本平台。

**如何获取 signing key**

- 登录后台（链接由运营提供）
- 进入 **商户管理**，在对应商户下查看并配置 **签名密钥**，**充值回调地址**、**提现回调地址**也在此处设置

![signing_key.png](./signing_key.png)

**充值回调数据格式**

当商户下的地址发生充值并达到确认数后，平台会将以下 JSON 以 **POST** 方式发送到您配置的**充值 Webhook URL**：

```json
{
    "address": "THotYegdHdngfJBRMpzTiT8J8yV4XhBBfo",
    "txid": "336fc08775278c406ccd1506865d4ac4f1b20c44d7c0a349c4bced36a519ecd9",
    "time": 1735888620,
    "confirmations": 1,
    "chain": "tron",
    "height": 53310985,
    "tokenAddress": "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
    "tokenSymbol": "USDT",
    "tokenDecimal": 6,
    "tokenValue": 123.23
}
```

| 字段 | 说明 |
|------|------|
| address | 发生充值的收款地址 |
| txid | 交易哈希 |
| time | 交易时间戳 |
| confirmations | 当前确认数 |
| chain | 链标识，如 tron |
| height | 区块高度 |
| tokenAddress | 代币合约地址 |
| tokenSymbol | 代币符号 |
| tokenDecimal | 代币精度 |
| tokenValue | 充值数量 |

**提现回调数据格式**

提现链上达到约定确认数后，平台会 POST 到您配置的**提现 Webhook URL**，数据格式与充值回调一致，并**多出 `memo` 字段**（即您调用提现申请时传入的备注）：

```json
{
    "address": "...",
    "txid": "...",
    "time": 1735888620,
    "confirmations": 1,
    "chain": "tron",
    "height": 53310985,
    "tokenAddress": "...",
    "tokenSymbol": "USDT",
    "tokenDecimal": 6,
    "tokenValue": 100.5,
    "memo": "123"
}
```

**响应要求**

- 您的服务器收到通知后，**HTTP 状态码必须为 200**，且**响应 body 必须返回字符串 `ok`**。
- 若未按要求响应，平台会自动重试，最多重试 3 次。

### 验证收到的签名

每个出站请求的 Header 中都会携带身份验证签名 **X-Signature**。签名的计算方式为：使用您的**签名密钥**与**请求 body 原文**作为输入，通过 **HMAC SHA256** 算法生成哈希值（十六进制字符串）。

**请求头示例**

```
Content-Type: application/json;charset=UTF-8
X-Signature: your-hashed-signature
```

接入方应使用相同方式计算签名，并与收到的 `X-Signature` 比较，一致则说明请求来自本平台。

**签名验证示例（Python）**

```python
import hmac
import hashlib

# data：webhook 请求的 body 原始字符串
data = bytes('abcde', 'utf-8')
# key：商户在后台配置的 signing key
key = bytes('signkey string', 'utf-8')
# digest 即为 X-Signature 的值
digest = hmac.new(key, data, digestmod=hashlib.sha256).hexdigest()
print(digest)
```

---

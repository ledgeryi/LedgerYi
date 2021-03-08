<p align="center">
<a target="_blank" rel="noopener noreferrer"><img src="https://github.com/ledgeryi/LedgerYi/blob/master/doc/logo.png" alt="" style="max-width:100%;"></a>
</p>


<p align="center">
<a> <img src="https://www.codefactor.io/repository/github/ledgeryi/LedgerYi/badge" alt="CodeFactor" /></a>
<a> <img src="https://tokei.rs/b1/github/ledgeryi/LedgerYi" alt="tokei" /></a>
</p>

# LedgerYi
LedgerYi是一个灵活、极简、开箱即用的开源分布式账本框架，可用于私有链、公有链、联盟链各个场景的区块链平台构建。

LedgerYi 具有以下特性： 
1. 开箱即用，2分钟一键启动 
2. 极简易用，10分钟上手，快速进入业务开发。
3. 开源开放，社区开源共建，链改零门槛。     
4. 生态互联，解构局域网式的”区块链孤岛”，生态开放、价值互联。

# RoadMap
LedgerYi版本规划分为五个阶段：Incubation、Workable、Harmony、Autonomy、Rebirth，各版本核心功能如下：

![](./doc/raodmap.jpg)

# 网络和部署说明

### 网络结构
LedgerYi网络采用Peer-to-Peer(P2P)的网络架构，网络中的节点地位对等。
网络中的节点有MasterNode、FollowerNode两个种类型，MasterNode作为共识节点，主要用于生成区块，FollowerNode用于同步区块、广播交易。

### 环境准备

- 开放UDP端口号，安装Docker
- 服务器最低配置要求，CPU：2核 内存：4G  硬盘：50G
- 安装Oracle JDK1.8，暂不支持JDK1.9+，且不支持Open JDK

## 使用镜像快速部署
### 拉取镜像

```text
docker pull ledgeryi/ledgeryi:alpha
```

### 准备config.conf文件

- 在node模块中获取config.conf配置文件，修改创世块内容、网络链接信息、签名算法等配置信息。
- 将修改好的config.conf文件拷贝到'/opt/ledgeryi/data'路径下(路径可自定义)。

### 启动容器
```text
docker run --rm --name="ledgeryi-master" -d \
      -p 6667:6666 -p 50052:50051 -p 8081:8080 \
      -v /opt/ledgeryi/data:/ledgeryi/data \
      ledgeryi/ledgeryi:alpha -m -c /ledgeryi/data/config.conf
```

命令行参数说明：
- `-m`用来指定该节点是否是超级（共识）节点，默认为false，即FollowerNode
- `-c`用来指定自定义配置文件路径

## 使用JAR包部署
### 克隆代码

**1.克隆最新代码**
```text
git clone https://github.com/chainboard/LedgerYi.git
```

**2.切换到master分支**
```text
cd LegerYi
git checkout -t origin/master
```

**3.编译代码**
```text
mvn clean package -DskipTests
```

### 启动节点
**1.启动MasterNode**

(1)在node模块中获取config.conf配置文件

(2)在localMaster中添加自己的私钥，并在localMasterAccountAddress中设置私钥对应的地址

(3)设置genesis.block.masters中的MasterNode的地址等信息

(4)第1个MasterNode设置needSyncCheck为false，其他可以设置为true

(5)设置node.discovery.enable为true

(6)使用以下命令运行MasterNode
```text
java -Xms128m -Xmx2048m -jar node-1.0-SNAPSHOT.jar --master -c config.conf
```
命令行参数说明：
- `--master`用来指定该节点是否是超级（共识）节点，默认为false，即FollowerNode
- `-c`用来指定配置文件路径

**2.启动FollowerNode**

(1)在node模块中获取config.conf配置文件

(2)设置seed.node.ip.list为MasterNode的地址和端口

(3)设置genesis.block与MasterNode中的genesis.block配置一致 

(4)设置needSyncCheck为true

(5)设置node.discovery.enable为true

(6)使用以下命令运行FollowerNode
```text
java -jar node-1.0-SNAPSHOT.jar -c config.conf
```

# 使用IDEA运行MasterNode节点

首先克隆代码到本地，然后切换到master分支。

### 配置IDEA开发环境

- 安装Oracle JDK1.8，暂时不支持Open JDK
- 安装Lombok插件

### 使用步骤

**1.克隆最新代码**
```text
git clone https://github.com/ledgeryi/LedgerYi.git
```

**2.切换到master分支**
```text
cd LedgerYi
git checkout -t origin/master
```

**3.编译代码**
```text
mvn clean compile -DskipTests
```

**4.启动MasterNode节点**

编译成功后，通过`cn.ledgeryi.framework.program.LedgerYiNode.java`路径找到主函数文件，启动一个全节点。

如果需要启动一个共识节点，可手动修改`Args.java`类的字段`private boolean master = true;`,master默认为false。

或者在Run/Debug Configurations的Application选项Program arguments添加参数`--master`。

启动成功后，可查看日志验证是否启动成功，日志路径为`/logs/node.log`。

# API接口说明

### HTTP API接口

启用HTTP API接口功能，需要在配置文件config.conf中设置`node.http.ledgerYiNodeEnable = true`，默认的http端口是8090。
http api接口请求格式：'http://127.0.0.1:8090/v1/listnodes'

**节点**

**(1)getnodes**

|类别|说明|
|:---|:---|
|作用|查询P2P对等节点|
|示例|curl -X POST  http://127.0.0.1:8090/v1/getnodes|
|参数说明|无|
|返回值|节点列表|

**(2)getnodeinfo**

|类别|说明|
|:---|:---|
|作用|查看当前节点的信息|
|示例|curl  http://127.0.0.1:8090/v1/getnodeinfo|
|返回值|节点当前状态的相关信息(省略)|


**(3)getmasters**

|类别|说明|
|:---|:---|
|作用|查询Master节点列表|
|示例|curl -X POST  http://127.0.0.1:8090/v1/getmasters|
|参数说明|无|
|返回值|所有Master节点列表|

**账户**

**(1)getaccount**

|类别|说明|
|:---|:---|
|作用|查询一个账号的信息|
|示例|curl -X POST  http://127.0.0.1:8090/v1/getaccount -d '{"address": "4e23514ccc74f7e10fe7da1c84346a98f14de1b65a"}'|
|参数说明|address默认为hexString|
|返回值|Account对象|


**区块**

**(1)getnowblock**

|类别|说明|
|:---|:---|
|作用|查询最新block|
|示例 |curl -X POST  http://127.0.0.1:8090/v1/getnowblock|
|参数说明|无|
|返回值|该节点账本的最新block|

**(2)getblockbynum**

|类别|说明|
|:---|:---|
|作用|按照高度查询block|
|示例|curl -X POST  http://127.0.0.1:8090/v1/getblockbynum -d '{"num" : 100}'|
|参数说明|num是块的高度|
|返回值|指定高度的block|

**(3)getblockbylimitnext**

|类别|说明|
|:---|:---|
|作用|按照范围查询block|
|示例|curl -X POST http://127.0.0.1:8090/v1/getblockbylimitnext -d '{"startNum": 1, "endNum": 2}'|
|参数说明|startNum：起始块高度，包含此块；endNum：截止块高度，不包含此此块|
|返回值|指定范围的block列表|


**交易**

**(1)createtransaction**

|类别|说明|
|:---|:---|
|作用|创建一个转账Transaction(，如果转账的to地址不存在，则在区块链上创建该账号)|
|示例|curl -X POST http://127.0.0.1:8090/v1/createtransaction -d '{"to_address":  <br>"4e23514ccc74f7e10fe7da1c84346a98f14de1b65a", "owner_address": "4e50381e6c0ad007d59b321f2f39f8800e43045a1a", "amount": 1}'|
|参数说明|to_address是转账转入地址，默认为hexString；owner_address是转账转出地址，默认为hexString；amount是转账数量|
|返回值|转账合约|

**(2)broadcasttransaction**

|类别|说明|
|:---|:---|
|作用|对签名后的transaction进行广播|
|示例|curl -X POST http://127.0.0.1:8090/v1/broadcasttransaction -d '{"signature":  <br>["97c825b41c77de2a8bd65b3df55cd4c0df59c307c0187e42321dcc1cc455ddba583dd9502e17cfec5945b34cad0511985a6165999092a6dec84c2bdd97e649fc01"],<br>"txID":"e5bdf8fb25fc9b93b8f8bda4ff0356b54f46ea9e6211ad31cd5101125c6a2012", <br>"raw_data":{"contract":[{"parameter":{"value":{"amount":1,"owner_address":"4e50381e6c0ad007d59b321f2f39f8800e43045a1a",<br>"to_address":"4e23514ccc74f7e10fe7da1c84346a98f14de1b65a"},<br>"type_url":"type.googleapis.com/protocol.TransferContract"},"type":"TransferContract"}], <br>"ref_block_bytes":"0047","ref_block_hash":"9465d7aff769ec1c","expiration":1607675676000,"timestamp":1607913591421},<br>"raw_data_hex":"0a02004722089465d7aff769ec1c40e08ac087e52e5a631a610a2d747970652e676f6f676c656<br>17069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412300a15d8598fe888d5e6ca98450730518e5b439760d<br>b7db712159e127afced01bdfb90db1769dac6f79658966015aa180170fda4f9f8e52e"}|
|参数说明|签名之后的Transaction|
|返回值|广播是否成功|

**(3)gettransactionbyid**

|类别|说明|
|:---|:---|
|作用|通过ID查询交易|
|示例|curl -X POST  http://127.0.0.1:8090/v1/gettransactionbyid -d <br>'{"value": "d5ec749ecc2a615399d8a6c864ea4c74ff9f523c2be0e341ac9be5d47d7c2d62"}'|
|参数说明|交易ID|
|返回值|交易信息|

**(4)gettransactioninfobyid**

|类别|说明|
|:---|:---|
|作用|通过ID查询交易信息|
|示例|curl -X POST  http://127.0.0.1:8090/v1/gettransactioninfobyid -d <br>'{"value": "d5ec749ecc2a615399d8a6c864ea4c74ff9f523c2be0e341ac9be5d47d7c2d62"}'|
|参数说明|交易ID|
|返回值|交易信息|

### RPC API接口

**1.区块**

**(1)getNowBlock**

作用:查询最新区块

```text
rpc GetNowBlock (EmptyMessage) returns (Block) {}
```

**(2)getBlockByNum**

作用：查询指定高度的区块

```text
rpc GetBlockByNum (NumberMessage) returns (Block) {}
```

**(3)getBlockByLimitNext**

```text
rpc GetBlockByLimitNext (BlockLimit) returns (BlockListExtention) {}
```


**2.交易**

**(1)getTransactionCountByBlockNum**

作用：查询指定区块高度的区块中包含的交易数

```text
rpc GetTransactionCountByBlockNum (NumberMessage) returns (NumberMessage) {}
```

**(2)getTransactionById**

作用：查询指定交易hash的交易

```text
rpc GetTransactionById (BytesMessage) returns (Transaction) {}
```

**(3)getTransactionInfoById**

作用：查询指定交易hash的交易

```text
rpc getTransactionInfoById (BytesMessage) returns (TransactionInfo) {}
```

**(4)createTransaction**

作用：创建交易

```text
rpc CreateTransaction (SystemContract) returns (TransactionExtention) {}
```

**(5)broadcastTransaction**

作用：广播交易

```text
rpc BroadcastTransaction (Transaction) returns (Return) {}

```
**3.账户**

**(1)getAccount**

作用：通过accountId查询一个账号的信息
```text
rpc GetAccount (Account) returns (Account) {}
```

**(2)getMasters**

作用：查询MasterNode列表

```text
rpc rpc GetMasters (EmptyMessage) returns (MastersList) {}
```

**5.节点**

**(1)listNodes**

作用：查询api所在机器连接的节点

```text
rpc ListNodes (EmptyMessage) returns (NodeList) {}
```

**(2)getNodeInfo**

作用：查看节点的信息

```text
rpc GetNodeInfo (EmptyMessage) returns (NodeInfo) {}
```
# License

LedgerYi的开源协议为LGPL-V3.0，详情参见[LICENSE](https://github.com/ledgeryi/LedgerYi/blob/master/LICENSE)。




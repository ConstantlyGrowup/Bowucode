--1.参数列表
--1.1 展览id
local resId=ARGV[1]
--1.2 用户id
local userId=ARGV[2]

--2.数据KEY
--2.1 展览库存KEY
local stockKey="cache:Reserve:Stock:" .. resId
--2.2 展览预约KEY
local orderKey="cache:Reserve:Order:" .. resId

--3.脚本业务
--3.1 判断库存是否充足
if(tonumber(redis.call("get",stockKey))<=0)then
    --3.1.2 库存不足，返回1
    return 1
end
--3.2 判断用户是否已经预约
if(redis.call("sismember",orderKey,userId)==1)then
    --3.2.1 用户已经预约，返回2
    return 2
end
--3.3 预扣减库存
redis.call("incrby",stockKey,-1)
--3.4 保存预约记录
redis.call("sadd",orderKey,userId)
--3.5 返回结果
return 0
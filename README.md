# SlotBot
Telegram 中文老虎机 Bot

### How to build
```
git clone https://github.com/edward-p/SlotBot.git
cd SlotBot
./gradlew jar
```

### How to use
```
java -jar SlotBot.jar <TOKEN> <DATA_FILE_PATH>
```
e.g.

```
java -jar SlotBot.jar "00000000:AAHkGMEC8MbCdvr6IjJb4kEkP76bU8NOFQ8" $HOME/.cache/slotbot.cache
```

### Bot Command Sheet
```
newgame - 创建多人游戏
join - 加入多人游戏
leave - 离开多人游戏
roll - 开始多人游戏
setbets - 设置赌注
getbets - 获取赌注
balance - 获取当前账户筹码
bonus - 奖励筹码
transfer - 转帐
help - 如何游玩
```

### Bot Settings
- Inline Mode: on
- Inline Feedback: 100%



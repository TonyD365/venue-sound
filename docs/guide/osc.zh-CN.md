# OSC 遥控

和真实的数字调音台一样，VenueSound 的调音台可以在游戏外用 **OSC**（Open Sound Control，调音台和遥控 App
常用的协议）来控制。在 TouchOSC、Open Stage Control 这类平板 App 上推推子，或者用任何能发 OSC 的软件，
游戏里的调音台都会跟着动；游戏里的改动也会发回去，App 上的推子同样会跟着动。

## 设置

1. 先启动一次游戏。VenueSound 在**你自己电脑的 UDP 9000 端口**（127.0.0.1）上接收 OSC。设置在
   `config/venuesound-osc.properties`：

   | 设置 | 默认 | 含义 |
   |---|---|---|
   | `enabled` | `true` | 开关 OSC |
   | `port` | `9000` | 接收用的 UDP 端口 |
   | `bind` | `127.0.0.1` | `127.0.0.1`：只接受本机的 App。`0.0.0.0`：局域网里的平板、手机也可以 |
   | `reply_port` | `0` | 反馈发到哪个端口：`0` 表示发回 App 发送时用的端口；否则填 App 的接收端口 |

2. 在 OSC App 里，目标填你电脑的地址和 9000 端口。用平板时把 `bind` 改成 `0.0.0.0`，重启游戏，
   地址填电脑的局域网 IP（比如 192.168.1.20）。
3. 在游戏里**右键一下想控制的那台调音台上任意一个控件**，会提示 OSC 遥控已经接管这台调音台。之后它一直
   跟随你最后用过的那台。

OSC 只在你自己的电脑上运行，和 Theatrical 的 Art-Net 一样：服务器不用多开端口，改动通过正常的游戏连接
发过去。离调音台 64 格以内都能控制。

## 地址

数值是 0 到 1 的浮点数（整数、true/false 也可以）。通道号从左往右数，从 1 开始：VS-12 上 1–8 是单声道
输入，9 是 USB 播放器（9/10），10 是立体声输入 11/12；VS-24 上 1–16 是单声道，17 是 USB，18–20 是立体声。

| 地址 | 控件 | 0 → 1 |
|---|---|---|
| `/venuesound/ch/N/fader` | 通道推子 | 最低 → 最高（0.5 = 0 dB） |
| `/venuesound/ch/N/gain` | 增益 | 最小 → 最大 |
| `/venuesound/ch/N/hi`、`/mid`、`/lo` | 均衡 | -12 dB → +12 dB（0.5 = 平直） |
| `/venuesound/ch/N/aux1`、`/aux2`、`/efx` | 辅助 / 效果发送 | 关 → 最大 |
| `/venuesound/ch/N/pan` | 声像 | 左 → 右（0.5 = 居中） |
| `/venuesound/ch/N/mute`、`/pfl` | 按钮 | 关 / 开（≥ 0.5 为开） |
| `/venuesound/main/l`、`/main/r` | 主推子 | 最低 → 最高 |
| `/venuesound/main` | 两个主推子一起 | 最低 → 最高 |
| `/venuesound/aux/1/master`、`/aux/2/master` | AUX 总推子 | 最低 → 最高 |
| `/venuesound/efx/return`、`/efx/time` | 效果返回、延时时间 | |
| `/venuesound/phones` | 耳机音量 | |
| `/venuesound/phantom` | +48V | 关 / 开 |

给百灵达 X32 / Midas M32 做的 App 和模板，也能用这些 X32 风格的地址：`/ch/01/mix/fader`、
`/ch/01/mix/on`（1 = 开，0 = 静音）、`/ch/01/mix/pan`、`/main/st/mix/fader`。只支持这一部分：需要完整
X32 协议的软件（比如 X32-Edit）连不上。

## 反馈

收到 App 发来的第一条消息后，VenueSound 会把每个变化了的控件用同样的地址发回去（每秒 5 次），
App 上的推子就会跟着游戏动。

## 例子

在终端里用 Python：

```python
import socket, struct
def osc(addr, v):
    pad = lambda b: b + b"\0" * (4 - len(b) % 4)
    return pad(addr.encode()) + pad(b",f") + struct.pack(">f", v)
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.sendto(osc("/venuesound/ch/1/fader", 0.5), ("127.0.0.1", 9000))   # 第 1 路推到 0 dB
s.sendto(osc("/ch/02/mix/on", 0), ("127.0.0.1", 9000))              # 第 2 路静音
```

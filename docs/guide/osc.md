# OSC remote control

Like a real digital console, the VenueSound console can be run from outside the game over **OSC**
(Open Sound Control), the protocol mixing consoles and remote apps use. Move faders on a tablet
app such as TouchOSC or Open Stage Control, or from any software that sends OSC, and the console
in the game follows; changes made in the game are sent back, so the app's faders move too.

## Setting up

1. Start the game once. VenueSound listens for OSC on **UDP port 9000 on your own computer**
   (127.0.0.1). The settings are in `config/venuesound-osc.properties`:

   | Setting | Default | Meaning |
   |---|---|---|
   | `enabled` | `true` | Turn OSC on or off |
   | `port` | `9000` | The UDP port VenueSound listens on |
   | `bind` | `127.0.0.1` | `127.0.0.1`: apps on this computer only. `0.0.0.0`: also a tablet or phone on your network |
   | `reply_port` | `0` | Where feedback goes: `0` answers to the port the app sends from; set the app's receive port otherwise |

2. In your OSC app, send to your computer's address and port 9000. For a tablet, set
   `bind=0.0.0.0`, restart the game and use your computer's network address (for example
   192.168.1.20).
3. In the game, **right-click any control on the console** you want to run. A message confirms
   that the OSC remote now controls that console. It keeps following the console you last used.

OSC runs on your own computer, like Theatrical's Art-Net: the server needs no extra port, and
changes reach it as normal game packets. You can control a console up to 64 blocks away.

## Addresses

Values are floats from 0 to 1 (ints and true/false work too). Channel numbers count the strips
from the left, starting at 1: on the VS-12, 1-8 are the mono inputs, 9 is the USB player (9/10)
and 10 is the stereo input 11/12; on the VS-24, 1-16 mono, 17 USB, 18-20 stereo.

| Address | Control | 0 → 1 |
|---|---|---|
| `/venuesound/ch/N/fader` | Channel fader | bottom → top (0.5 = 0 dB) |
| `/venuesound/ch/N/gain` | GAIN | min → max |
| `/venuesound/ch/N/hi`, `/mid`, `/lo` | EQ | -12 dB → +12 dB (0.5 = flat) |
| `/venuesound/ch/N/aux1`, `/aux2`, `/efx` | Sends | off → full |
| `/venuesound/ch/N/pan` | PAN | left → right (0.5 = centre) |
| `/venuesound/ch/N/mute`, `/pfl` | Buttons | off / on (≥ 0.5 = on) |
| `/venuesound/main/l`, `/main/r` | MAIN faders | bottom → top |
| `/venuesound/main` | Both MAIN faders together | bottom → top |
| `/venuesound/aux/1/master`, `/aux/2/master` | AUX masters | bottom → top |
| `/venuesound/efx/return`, `/efx/time` | Effects return and delay time | |
| `/venuesound/phones` | Headphone level | |
| `/venuesound/phantom` | +48V | off / on |

For apps and templates made for Behringer X32 / Midas M32 consoles, these X32-style addresses
work as well: `/ch/01/mix/fader`, `/ch/01/mix/on` (1 = on, 0 = muted), `/ch/01/mix/pan` and
`/main/st/mix/fader`. Only this subset: apps that need the full X32 protocol (such as X32-Edit)
won't connect.

## Feedback

After the first message from an app, VenueSound sends back every control that changes (five
times a second), on the same addresses, so faders on the app follow the game.

## Example

From a terminal, with Python:

```python
import socket, struct
def osc(addr, v):
    pad = lambda b: b + b"\0" * (4 - len(b) % 4)
    return pad(addr.encode()) + pad(b",f") + struct.pack(">f", v)
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.sendto(osc("/venuesound/ch/1/fader", 0.5), ("127.0.0.1", 9000))   # channel 1 to 0 dB
s.sendto(osc("/ch/02/mix/on", 0), ("127.0.0.1", 9000))              # mute channel 2
```

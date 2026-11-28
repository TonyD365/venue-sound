# Every item and block

Every item and block VenueSound adds: what it is for, its recipe, how to place it, its controls
and its sockets. For a shorter overview, see [devices](devices.md); for the console's controls in
detail, see [the mixing console](console.md).

How to read this page:

- **Facing**: unless a section says otherwise, a device is placed with its front towards you.
- **Sockets**: blue rings are **IN**, red rings are **OUT**. Plug cables into them with any
  cable (see [cabling](cabling.md)).
- **Controls**: right-click a button or switch; click the left half of a knob to turn it down and
  the right half to turn it up; hold right-click on a fader and move the crosshair. The mouse
  wheel turns the knob or fader under the crosshair one step per notch.
- **Recipes**: a 3×3 crafting grid, `.` is an empty slot. Every item also comes from the
  **VenueSound** creative tab.

## Contents

- [Sources](#sources):
  [Wireless Microphone](#wireless-microphone) ·
  [Headset Mic](#headset-mic) ·
  [Lavalier Mic](#lavalier-mic) ·
  [Wireless Receiver](#wireless-receiver) ·
  [Stand Microphone](#stand-microphone) ·
  [Gooseneck Podium Mic](#gooseneck-podium-mic) ·
  [Hanging Choir Mic](#hanging-choir-mic) ·
  [Media Deck](#media-deck) ·
  [Video Capture](#video-capture)
- [Mixing](#mixing):
  [Mixing Console VS-12](#mixing-console-vs-12) ·
  [Mixing Console VS-24](#mixing-console-vs-24)
- [Rack](#rack):
  [Power Amplifier](#power-amplifier) ·
  [Graphic Equalizer](#graphic-equalizer) ·
  [Compressor and Limiter](#compressor-and-limiter) ·
  [Effects Processor](#effects-processor) ·
  [Crossover and Speaker Processor](#crossover-and-speaker-processor) ·
  [Power Sequencer](#power-sequencer) ·
  [Rack Cabinet](#rack-cabinet)
- [Speakers](#speakers):
  [Line Array Module](#line-array-module) ·
  [Flown Line Array](#flown-line-array) ·
  [Truss Hanging Point](#truss-hanging-point) ·
  [PA Speaker Cabinet](#pa-speaker-cabinet) ·
  [Concert Speaker Stack](#concert-speaker-stack) ·
  [Floor Wedge Monitor](#floor-wedge-monitor) ·
  [Subwoofer](#subwoofer) ·
  [In-Ear Monitor Transmitter](#in-ear-monitor-transmitter) ·
  [In-Ear Monitor Earpiece](#in-ear-monitor-earpiece)
- [Intercom](#intercom):
  [Intercom Base Station](#intercom-base-station) ·
  [Intercom Beltpack](#intercom-beltpack)
- [Cabling and patching](#cabling-and-patching):
  [How every cable works](#how-every-cable-works) ·
  [Audio Cable](#audio-cable) ·
  [XLR Mic Cable](#xlr-mic-cable) ·
  [Speaker Cable](#speaker-cable) ·
  [Multicore Snake](#multicore-snake) ·
  [Cable Reel](#cable-reel) ·
  [Cable Clip](#cable-clip) ·
  [Wall Outlet](#wall-outlet) ·
  [Stage Box 8 ch](#stage-box-8-ch) ·
  [Stage Box 16 ch](#stage-box-16-ch) ·
  [DI Box](#di-box)
- [Tools](#tools):
  [Zone Selector](#zone-selector)

## Sources

### Wireless Microphone

A handheld radio mic, like the one a singer or presenter carries. While you hold it, your voice
goes to every Wireless Receiver on the same channel within 128 blocks.

**Recipe** (1 mic):

```
. B .
. R .
. K .
```

B = Iron Bars, R = Redstone Dust, K = Black Concrete.

**Use**: hold it in either hand and talk (Simple Voice Chat). It has no placed form.

| Action | Does |
|---|---|
| Right-click | Next channel (1-16, then back to 1). The channel shows above the hotbar |
| Sneak + right-click | Mute / unmute. The tooltip shows Live or Muted |

**Hookups**: set it to the channel of a [Wireless Receiver](#wireless-receiver); the receiver is
cabled to the console.

### Headset Mic

A headset mic with a beltpack, as worn by a presenter or a dancing singer. Hands-free: it
transmits while worn or carried anywhere in your inventory.

**Recipe** (1 mic):

```
S . S
. I .
R G R
```

S = String, I = Iron Ingot, R = Redstone Dust, G = Glowstone Dust.

| Action | Does |
|---|---|
| Right-click | Wear it on your head |
| Sneak + right-click | Next channel (1-16) |

A worn mic has no mute. To go silent, take it off and put it away in a chest, or switch it to a
channel no receiver uses. If you carry several mics, the one in your hand is used first, then
the worn one, then one in your inventory.

**Hookups**: set it to a [Wireless Receiver](#wireless-receiver)'s channel.

### Lavalier Mic

A small clip-on mic with a beltpack, as used in theatre and for speeches. Works like the
[Headset Mic](#headset-mic), but is worn on the chest.

**Recipe** (1 mic):

```
. I .
. S .
R G R
```

I = Iron Ingot, S = String, R = Redstone Dust, G = Glowstone Dust.

| Action | Does |
|---|---|
| Right-click | Wear it on your chest |
| Sneak + right-click | Next channel (1-16) |

It transmits while worn or carried. No mute on a worn mic.

**Hookups**: set it to a [Wireless Receiver](#wireless-receiver)'s channel.

### Wireless Receiver

The receiver for radio mics. It picks up every wireless, headset and lavalier mic on its channel
within 128 blocks and sends the voice into a cable.

**Recipe** (1 receiver):

```
L . L
I R I
I I I
```

L = Lightning Rod, I = Iron Ingot, R = Redstone Dust.

**Placing**: a small, low box (less than a block) on a desk or in a rack. The front faces you.

**Controls** (front panel):

| Control | Does |
|---|---|
| Channel display | Right-click: next channel |
| - button | Previous channel |
| + button | Next channel |

**Sockets**:

| Socket | Direction | Face |
|---|---|---|
| OUT | OUT | Back |

**Hookups**: OUT to a console IN with an [XLR Mic Cable](#xlr-mic-cable), or to a
[Stage Box](#stage-box-8-ch) or [Wall Outlet](#wall-outlet). Use one receiver per mic, each on
its own channel.

### Stand Microphone

A wired vocal mic on a floor stand. It picks up players talking within 3 blocks of it.

**Recipe** (1 mic):

```
. M .
. I .
I I I
```

M = Wireless Microphone, I = Iron Ingot.

**Placing**: one block, standing on the floor. It has no controls.

**Sockets**:

| Socket | Direction | Face |
|---|---|---|
| OUT | OUT | Any side: right-click anywhere on the mic |

**Hookups**: [XLR Mic Cable](#xlr-mic-cable) to a console IN or a stage box CH socket.

### Gooseneck Podium Mic

The thin mic on a lectern or a conference desk. It picks up players within 2 blocks.

**Recipe** (1 mic):

```
. . I
S I .
I I I
```

I = Iron Ingot, S = String.

**Placing**: one block, on a lectern or desk. No controls.

**Sockets**: OUT, on any side (right-click anywhere on the mic).

**Hookups**: [XLR Mic Cable](#xlr-mic-cable) to a console IN.

### Hanging Choir Mic

An overhead mic for a choir or a group on stage. It picks up players within 8 blocks.

**Recipe** (1 mic):

```
. C .
. I .
. R .
```

C = Chain, I = Iron Ingot, R = Redstone Dust. (Any column of the grid works.)

**Placing**: one block, placed above the singers, for example under a ceiling or truss. You can
walk through it. No controls.

**Sockets**: OUT, on any side (right-click anywhere on the mic).

**Hookups**: [XLR Mic Cable](#xlr-mic-cable), usually along [cable clips](#cable-clip) on the
ceiling, to a console IN.

### Media Deck

A playback deck for walk-in music and sound effects. It plays `.mp3` and `.wav` files from the
`venuesound_media` folder (next to `mods/`; on a server, in the server folder).

**Recipe** (1 deck):

```
I I I
R J R
I I I
```

I = Iron Ingot, R = Redstone Dust, J = Jukebox.

**Placing**: half a block tall, on a desk or in a rack. The front faces you.

**Controls** (front panel, left to right under the display):

| Control | Does |
|---|---|
| Display | Shows the track and whether it is playing |
| Prev | Previous track |
| Play | Play the track |
| Stop | Stop |
| Next | Next track |
| Loop | Loop on / off |
| Sneak + right-click the deck | Opens the track list |

**Sockets**:

| Socket | Direction | Face |
|---|---|---|
| OUT | OUT | Back |

**Hookups**: OUT to a console IN (a mono strip, or through a [DI Box](#di-box)).

### Video Capture

Brings the sound of WATERFrAMES screens into the console. Screens inside its capture zone follow
the console's faders and mutes and are loud near the speakers. (Their sound is played by each
player's own computer, so it cannot come from the speakers' direction.)

**Recipe** (1 device):

```
I G I
R C R
I I I
```

I = Iron Ingot, G = Glowstone Dust, R = Redstone Dust, C = Redstone Comparator.

**Placing**: one block. The front faces you. It has no controls: its zone is set with the
[Zone Selector](#zone-selector).

**Sockets**:

| Socket | Direction | Face |
|---|---|---|
| OUT | OUT | Back |

**Hookups**: OUT to a console IN.

## Mixing

### Mixing Console VS-12

The front-of-house mixing desk: 8 mono strips and 2 stereo strips (ST1, strip 9/10, is the
console's built-in USB player), plus a master section. Every control is on its top. See
[the mixing console](console.md) for what each control does to the sound.

**Recipe** (1 console):

```
L C L
R Q R
I I I
```

L = Lever, C = Redstone Comparator, R = Redstone Dust, Q = Nether Quartz, I = Iron Ingot.

**Placing**: 3 blocks wide, 2 deep, half a block tall. You stand at the front (the operator's
side). The block you place it on becomes the front-left corner; the console extends 3 blocks to
your right and 2 blocks away from you, so it needs that space free. Breaking any part removes the
whole console.

**Controls** (top surface; each strip from the back to the front):

| Control | Range | Use |
|---|---|---|
| GAIN | 0-2, default 1 | Knob |
| HI / MID / LOW | ±12 dB, 2 dB steps | Knobs |
| AUX1 / AUX2 | 0-1 | Knobs: send to AUX 1 / AUX 2 |
| EFX | 0-1 | Knob: send to the effects unit |
| PAN | left - right | Knob |
| PEAK | | LED |
| MUTE | on / off | Button |
| PFL | on / off | Button |
| Fader | 0-2, middle mark = 0 dB | Hold and drag, or wheel |

Master section (right end): USB PLAYER display, DELAY knob (effects echo time) and PHONES knob
beside it, USB buttons |< (previous), > (play), [] (stop), >| (next), the L/R level meter, and
faders EFX (effects return), AUX1, AUX2, L and R (main).

Knobs and faders also turn with the mouse wheel (faders in steps of 0.1).

**Sockets**: on the rear side (facing the stage), each input behind its strip.

| Socket | Direction | Face |
|---|---|---|
| IN 1 - IN 8 | IN | Rear, behind strips 1-8 |
| IN 11/12 | IN | Rear, behind the second stereo strip |
| MAIN L, MAIN R | OUT | Rear, behind the master section |
| AUX 1, AUX 2 | OUT | Rear, behind the master section |

The first stereo strip (9/10) is the USB player and has no socket. Each output takes one cable.

**Hookups**: mics, receivers, stage box snake and media decks into the INs; MAIN L / MAIN R to an
amplifier's IN A / IN B (or through rack processors); AUX 1 / AUX 2 to the amplifier feeding the
floor wedges, or to an [In-Ear Monitor Transmitter](#in-ear-monitor-transmitter).

### Mixing Console VS-24

The larger desk: 16 mono strips and 4 stereo strips. Same controls as the
[VS-12](#mixing-console-vs-12).

**Recipe** (shapeless, 1 console): 2 × Mixing Console (VS-12).

**Placing**: 5 blocks wide, 2 deep. The block you place it on is the front-left corner; it
extends 5 blocks to your right and 2 away from you.

**Controls**: as on the VS-12, with 20 strips. The first stereo strip (17/18) is the USB player.

**Sockets** (rear side, facing the stage, each input behind its strip):

| Socket | Direction | Face |
|---|---|---|
| IN 1 - IN 16 | IN | Rear, behind strips 1-16 |
| IN 19/20, IN 21/22, IN 23/24 | IN | Rear, behind the stereo strips |
| MAIN L, MAIN R, AUX 1, AUX 2 | OUT | Rear, behind the master section |

**Hookups**: as the VS-12. A [Stage Box 16 ch](#stage-box-16-ch) snake fills IN 1-16 in one run.

## Rack

### Power Amplifier

Drives the speakers. **IN A** feeds **OUT 1** and **OUT 2**; **IN B** feeds **OUT 3** and
**OUT 4**, at the level set on the front. It is on when placed.

**Recipe** (1 amplifier):

```
I I I
B C B
I I I
```

I = Iron Ingot, B = Block of Redstone, C = Redstone Comparator.

**Placing**: one block, in a rack. The front faces you; the sockets are on the back.

**Controls** (front panel):

| Control | Does |
|---|---|
| Rocker switch | Power on / off |
| Level knob | 25 % to 200 % in 25 % steps (default 100 %). Left half down, right half up; hold to keep turning; wheel |

**Sockets**:

| Socket | Direction | Face |
|---|---|---|
| IN A, IN B | IN | Back, top row |
| OUT 1 - OUT 4 | OUT | Back, bottom row |

**Hookups**: console MAIN L → IN A, MAIN R → IN B; OUT 1 → left speaker, OUT 3 → right speaker,
with [Speaker Cable](#speaker-cable). Each OUT takes one cable.

### Graphic Equalizer

A 7-band EQ to tune the whole system to the room. It sits in a cable run, usually between the
console and the amplifier.

**Recipe** (1 unit):

```
I G I
R C R
I I I
```

I = Iron Ingot, G = Glowstone Dust, R = Redstone Dust, C = Redstone Comparator.

**Placing**: one block, in a rack. The front faces you.

**Controls** (front panel):

| Control | Does |
|---|---|
| Sliders 63, 160, 400, 1K, 2.5K, 6.3K, 16K | ±12 dB in 3 dB steps. Click the upper half to raise, the lower half to lower; wheel |
| Rocker switch | Power. Off: no signal passes |
| Level knob | Output level, 25 % to 200 % |

**Sockets** (same as the amplifier, all on the back): IN A, IN B (IN); OUT 1 - OUT 4 (OUT).
Both inputs go to all four outputs.

**Hookups**: console MAIN L → IN A, then OUT 1 → amplifier IN A.

### Compressor and Limiter

Evens out loud and quiet passages and stops peaks. In-line, like the EQ.

**Recipe** (1 unit):

```
I R I
R C R
I I I
```

I = Iron Ingot, R = Redstone Dust, C = Redstone Comparator.

**Placing**: one block, in a rack. The front faces you.

**Controls** (front panel):

| Control | Does |
|---|---|
| THRESH knob | 0.1-1, default 0.5. Lower squeezes more of the signal |
| RATIO knob | 1-10, default 4 |
| Rocker switch | Power |
| Level knob | Output level |

Knobs: left half down, right half up, or wheel.

**Sockets** (back): IN A, IN B (IN); OUT 1 - OUT 4 (OUT).

**Hookups**: between a console output and the amplifier, or between a mic and a console input.

### Effects Processor

An echo unit for vocals and speech. In-line.

**Recipe** (1 unit):

```
I E I
R C R
I I I
```

I = Iron Ingot, E = Ender Pearl, R = Redstone Dust, C = Redstone Comparator.

**Placing**: one block, in a rack. The front faces you.

**Controls** (front panel):

| Control | Does |
|---|---|
| MIX knob | Amount of echo, 0-1, default 0.4 |
| TIME knob | Echo time, 0-1, default 0.4 |
| Rocker switch | Power |
| Level knob | Output level |

**Sockets** (back): IN A, IN B (IN); OUT 1 - OUT 4 (OUT).

**Hookups**: in a cable run between the console and the amplifier. (The console also has its own
EFX send and echo.)

### Crossover and Speaker Processor

Splits the signal for subwoofers and top speakers. Use one crossover per feed.

**Recipe** (1 unit):

```
I C I
R C R
I I I
```

I = Iron Ingot, C = Redstone Comparator, R = Redstone Dust.

**Placing**: one block, in a rack. The front faces you.

**Controls** (front panel):

| Control | Does |
|---|---|
| FULL/LOW/HIGH knob | FULL: passes everything. LOW: lows only (for subwoofers). HIGH: lows cut (for tops) |
| Rocker switch | Power |
| Level knob | Output level |

**Sockets** (back): IN A, IN B (IN); OUT 1 - OUT 4 (OUT).

**Hookups**: console MAIN → crossover set to LOW → amplifier → [Subwoofer](#subwoofer); a second
crossover set to HIGH → amplifier → tops.

### Power Sequencer

Switches a whole system on and off in a safe order: on, speakers first and amplifiers last; off,
amplifiers first. Steps are 4 ticks apart.

**Recipe** (1 unit):

```
I L I
R C R
I I I
```

I = Iron Ingot, L = Lever, R = Redstone Dust, C = Redstone Comparator.

**Placing**: one block, in a rack. The front faces you.

**Controls**: right-click it. If any amplifier within 16 blocks is on, it switches every amplifier
and speaker within 16 blocks off; otherwise it switches them all on. Processors are not switched.

**Sockets**: none.

### Rack Cabinet

A rack to stand amplifiers and processors in. It looks the part and carries no signal.

**Recipe** (1 cabinet):

```
I I I
I . I
I I I
```

I = Iron Ingot.

**Placing**: one block. The front faces you. No controls or sockets.

## Speakers

Every speaker has its **INPUT** socket, a **power switch** and a **coverage knob** on its back
panel. Coverage is how far it is heard: 16, 32, 48, 64 (default), 96 or 128 blocks. Click the left
half of the knob to lower it, the right half to raise it, or use the wheel. Speakers are on when
placed. A speaker plays only what reaches its INPUT through cables.

### Line Array Module

A compact line array box for main PA or side fills.

**Recipe** (1 module):

```
I I I
N J N
I I I
```

I = Iron Ingot, N = Note Block, J = Jukebox.

**Placing**: one block. The front (the grille) faces you, so place it while standing where the
audience will be; the back panel then faces away from you.

| Socket / control | Direction | Face |
|---|---|---|
| INPUT | IN | Back |
| Power switch, coverage knob | | Back |

**Hookups**: amplifier OUT → INPUT with [Speaker Cable](#speaker-cable).

### Flown Line Array

A line array element hung above the stage.

**Recipe** (1 element):

```
. C .
P N P
P P P
```

C = Chain, P = any Planks, N = Note Block.

**Placing**: one block. It does not need support; place it under a
[Truss Hanging Point](#truss-hanging-point) so it looks hung. The front faces you.

| Socket / control | Direction | Face |
|---|---|---|
| INPUT | IN | Back |
| Power switch, coverage knob | | Back |

**Hookups**: amplifier OUT → INPUT with [Speaker Cable](#speaker-cable), along
[cable clips](#cable-clip) up the truss.

### Truss Hanging Point

A rigging point to hang flown speakers from. It looks the part and carries no signal.

**Recipe** (1 point):

```
I C I
. C .
```

I = Iron Ingot, C = Chain.

**Placing**: one block, at the top, with flown speakers below it. No controls or sockets.

### PA Speaker Cabinet

A full-range PA cabinet (woofers below, horn above), the usual main speaker for a small venue.

**Recipe** (1 cabinet):

```
I S I
W S W
I S I
```

I = Iron Ingot, S = Line Array Module, W = Black Wool.

**Placing**: 2 blocks tall; it needs a free block above. The front faces you. Breaking either half
removes it.

| Socket / control | Direction | Face |
|---|---|---|
| INPUT | IN | Back, lower half |
| Power switch, coverage knob | | Back, lower half |

**Hookups**: amplifier OUT 1 → left cabinet, OUT 3 → right cabinet.

### Concert Speaker Stack

A large stadium stack: horn arrays on top, 15" mid-bass drivers in the middle, 18" subwoofers at
the bottom.

**Recipe** (1 stack):

```
P P P
P A P
P P P
```

P = PA Speaker Cabinet, A = Power Amplifier.

**Placing**: 4 blocks wide, 6 tall and 2 deep. The front faces you. The block you place it on is
the bottom-left corner as you see it; the stack extends 4 blocks to your right, 6 up and 2 away
from you. Breaking any part removes the whole stack.

| Socket / control | Direction | Face |
|---|---|---|
| INPUT | IN | Back, bottom, behind the corner you placed it on |
| Power switch, coverage knob | | Same place |

**Hookups**: amplifier OUT → INPUT with [Speaker Cable](#speaker-cable). Use one each side of the
stage.

### Floor Wedge Monitor

A low, angled speaker on the stage floor, pointing back at a performer so they hear themselves.

**Recipe** (1 wedge):

```
P N P
P R P
P P P
```

P = any Planks, N = Note Block, R = Redstone Dust.

**Placing**: just over half a block tall, on the stage floor. Its front faces you, so place it
while standing where the performer will stand.

| Socket / control | Direction | Face |
|---|---|---|
| INPUT | IN | Back panel |
| Power switch, coverage knob | | Back panel |

**Hookups**: console AUX 1 → amplifier IN → amplifier OUT → INPUT. Keep coverage low (16) so the
audience does not hear it.

### Subwoofer

A low-frequency cabinet that adds the low end, usually on the floor under or beside the main
speakers.

**Recipe** (1 subwoofer):

```
P N P
N R N
P N P
```

P = any Planks, N = Note Block, R = Redstone Dust.

**Placing**: one block, on the floor. The front faces you.

| Socket / control | Direction | Face |
|---|---|---|
| INPUT | IN | Back |
| Power switch, coverage knob | | Back |

**Hookups**: amplifier OUT → INPUT. For a real sub feed, put a
[Crossover](#crossover-and-speaker-processor) set to LOW before its amplifier.

### In-Ear Monitor Transmitter

Sends a monitor mix by radio to performers' earpieces. Only players carrying an
[In-Ear Monitor Earpiece](#in-ear-monitor-earpiece) set to the same channel hear it, within at
least 128 blocks. Use one transmitter per mix: for example the singer's earpiece and transmitter
on channel 1 fed from AUX 1, the drummer's on channel 2 fed from AUX 2.

**Recipe** (1 transmitter):

```
I E I
R C R
```

I = Iron Ingot, E = Ender Pearl, R = Redstone Dust, C = Redstone Comparator.

**Placing**: a small, low box on a desk or in a rack. The front faces you.

| Socket / control | Direction | Face |
|---|---|---|
| INPUT | IN | Back panel |
| Power switch, coverage knob | | Back panel |
| Channel display | | Front: right-click for the next channel (1-16), sneak + right-click for the previous |

**Hookups**: console AUX 1 or AUX 2 → INPUT (directly, or through an amplifier).

### In-Ear Monitor Earpiece

The performer's earpiece for the [In-Ear Monitor Transmitter](#in-ear-monitor-transmitter).

**Recipe** (1 earpiece):

```
S . S
I . I
G . G
```

S = String, I = Iron Ingot, G = Glowstone Dust.

**Use**: keep it anywhere in your inventory to hear the in-ear transmitter on its channel. It has
no placed form.

| Action | Does |
|---|---|
| Right-click | Next channel (1-16). The tooltip shows the channel |
| Sneak + right-click | Previous channel |

## Intercom

The crew's private talk system: the director, stage manager, lighting and sound talk to each
other through headsets, and the audience never hears it. It does not go through the console.

### Intercom Base Station

The heart of the intercom. Beltpacks only work within its coverage, and whatever is cabled into
its program input (usually the console's MAIN or an AUX) is played to every beltpack nearby so
the crew can follow the show.

**Recipe** (1 station):

```
I N I
R C R
I I I
```

I = Iron Ingot, N = Note Block, R = Redstone Dust, C = Redstone Comparator.

**Placing**: a half-height box on a desk or in a rack. The front faces you.

| Socket / control | Direction | Face |
|---|---|---|
| PGM IN | IN | Back panel |
| Power switch | | Back panel: click to turn on or off |
| Coverage knob | | Back panel: 16-128 blocks (click halves or scroll). Beltpacks must be inside it |

**Hookups**: console AUX → PGM IN (optional). Without power the whole intercom is silent.

### Intercom Beltpack

A crew member's beltpack and headset. Keep it anywhere in your inventory.

**Recipe** (1 beltpack):

```
S L S
I R I
```

S = String, L = Leather, I = Iron Ingot, R = Redstone Dust.

| Action | Does |
|---|---|
| Right-click | Switch channel A / B |
| Sneak + right-click | TALK on / off. With TALK on, everyone carrying a beltpack on your channel hears you |

You always hear the people talking on your channel and the station's program feed. Both you and
the listener must be within a powered base station's coverage, in the same dimension. Two
channels let two teams work at once, for example A for stage management and B for lighting.

## Cabling and patching

### How every cable works

1. Right-click an **OUT** socket with the cable. One end plugs in and the cable runs to your hand.
2. Optionally right-click [cable clips](#cable-clip) one after another to run the cable along
   them.
3. Right-click an **IN** socket to plug in the other end.

| Action | Does |
|---|---|
| Sneak + right-click a socket | Unplugs the cable there |
| Sneak + right-click the air | Drops the end you are holding |

A cable is not used up: one cable item makes as many runs as you like. The length limit is per
run (socket to clip, clip to clip, clip to socket). A cable that would go through a wall is
refused: route it along clips.

| Cable | Carries | Per run | Colour |
|---|---|---|---|
| [Audio Cable](#audio-cable) | Anything | 64 blocks | Black |
| [XLR Mic Cable](#xlr-mic-cable) | Mic and line signal; not from an amplifier's outputs | 96 blocks | Dark blue |
| [Speaker Cable](#speaker-cable) | Only amplifier OUT to speaker INPUT (wall outlets allowed in between) | 96 blocks | Orange |
| [Multicore Snake](#multicore-snake) | Only a stage box's SNAKE socket to the console (or a wall outlet) | 128 blocks | Thick black |
| [Cable Reel](#cable-reel) | Anything | 128 blocks | Yellow |

### Audio Cable

The general-purpose cable. It connects anything to anything.

**Recipe** (1 cable):

```
S . .
. R .
. . S
```

S = String, R = Redstone Dust.

64 blocks per run, drawn black.

### XLR Mic Cable

The standard mic cable. For mics, receivers, media decks and console outputs into processors
and amplifiers. It cannot start at an amplifier's outputs: use speaker cable there.

**Recipe** (1 cable):

```
. W .
. R .
. W .
```

W = Black Wool, R = Redstone Dust. (Any column works.)

96 blocks per run, drawn dark blue.

### Speaker Cable

Heavy cable from an amplifier to a speaker. It only runs from an amplifier OUT to a speaker
INPUT; [Wall Outlets](#wall-outlet) in between are allowed.

**Recipe** (1 cable):

```
. W .
. C .
. W .
```

W = Black Wool, C = Redstone Comparator. (Any column works.)

96 blocks per run, drawn orange.

### Multicore Snake

The thick multi-channel cable from the stage to the mixing position. It only runs from a stage
box's **SNAKE** socket to a console input (or to a wall outlet on the way).

**Recipe** (1 snake):

```
W W W
R C R
W W W
```

W = Black Wool, R = Redstone Dust, C = Redstone Comparator.

128 blocks per run, drawn thick black. Plug the console end into **IN 1**: stage box CH 1 lands
on IN 1, CH 2 on IN 2 and so on (the USB strip is skipped).

### Cable Reel

A long general-purpose cable on a drum, for long runs. It connects anything.

**Recipe** (1 reel):

```
P W P
W C W
P W P
```

P = any Planks, W = Black Wool, C = Redstone Comparator.

128 blocks per run, drawn yellow.

### Cable Clip

A small clip that fixes cables to a wall, floor or ceiling, so runs follow the building instead
of hanging across the room.

**Recipe** (8 clips):

```
N . N
N N N
```

N = Iron Nugget.

**Placing**: right-click any side of any block (not air or liquid). It sticks to that side. You
can walk through it.

**Use**: while holding a cable that is plugged in at one end, right-click clips one after
another, then right-click the IN socket. Each clip is a corner of the run; each stretch between
clips can be as long as the cable's run length.

### Wall Outlet

A passive patch point: a plate with four sockets, like the wall boxes that link a stage to a
control room. Cables run into it and on to other outlets, a console, an amplifier or speakers.
One outlet can also split one signal to several cables.

**Recipe** (4 outlets):

```
N N N
N R N
N N N
```

N = Iron Nugget, R = Redstone Dust.

**Placing**: right-click a wall and the plate sits on that wall, facing out of it. Placed on a
floor or ceiling, it faces you.

**Sockets** (on the plate):

| Socket | Direction | Face |
|---|---|---|
| S1, S2, S3, S4 | IN or OUT (each socket takes either) | Front (the plate) |

Up to 8 cables can leave one outlet.

**Hookups**: stage receiver OUT → outlet S1 on the stage wall; outlet S2 on the same wall → outlet
S1 in the control room; that outlet's S2 → console IN. Speaker cable may pass through outlets.

### Stage Box 8 ch

A box on the stage that collects up to 8 mics and sends them to the console down one
[Multicore Snake](#multicore-snake).

**Recipe** (1 box):

```
I I I
W C W
I I I
```

I = Iron Ingot, W = Black Wool, C = Redstone Comparator.

**Placing**: like a wall outlet: on a wall it faces out of the wall; on the floor it faces you.

**Sockets** (all on the front):

| Socket | Direction | Face |
|---|---|---|
| CH 1 - CH 8 | IN | Front, two rows of four |
| SNAKE | OUT | Front, bottom |

**Hookups**: mics → CH 1-8 with [XLR Mic Cable](#xlr-mic-cable); SNAKE → console IN 1 with a
Multicore Snake. CH 1 reaches the input the snake is plugged into, CH 2 the next one, and so on.

### Stage Box 16 ch

The 16-channel stage box, for the [VS-24](#mixing-console-vs-24).

**Recipe** (1 box):

```
W I W
W C W
W I W
```

W = Black Wool, I = Iron Ingot, C = Redstone Comparator.

**Placing**: as the 8-channel box.

**Sockets** (front): CH 1 - CH 16 (IN, two rows of eight); SNAKE (OUT, bottom).

**Hookups**: mics → CH 1-16; SNAKE → VS-24 IN 1.

### DI Box

A direct box: takes a line-level source (a media deck, a keyboard) and feeds it to a mic input.
It passes the signal on at its level setting.

**Recipe** (1 box):

```
I . I
R C R
I I I
```

I = Iron Ingot, R = Redstone Dust, C = Redstone Comparator.

**Placing**: one block, on the stage floor or in a rack. The front faces you.

**Controls** (front panel): rocker switch (power), level knob (25 % to 200 %).

**Sockets** (back): IN A, IN B (IN); OUT 1 - OUT 4 (OUT).

**Hookups**: Media Deck OUT → DI IN A; DI OUT 1 → stage box CH or console IN.

## Tools

### Zone Selector

Marks the capture zone of a [Video Capture](#video-capture) device: the area whose WATERFrAMES
screens play through the console.

**Recipe** (1 selector):

```
. . G
. . I
. I .
```

G = Glowstone Dust, I = Iron Ingot.

| Action | Does |
|---|---|
| Right-click a block | First corner. Right-click a second block: second corner (at most 48 blocks per side). A further click starts a new selection |
| Right-click a Video Capture block | Gives it the selected zone |
| Sneak + right-click the air | Clears the selection |

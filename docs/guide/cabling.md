# Cabling

## Plugging in

1. Right-click an **OUT** socket (red ring) with a cable: one end is plugged in, and the cable
   runs to your hand.
2. Optionally right-click **cable clips** to run it along walls, floors or ceilings. A clip
   sticks to any side of any block (walls, glass, fences, trusses, slabs, devices), not to air.
3. Right-click an **IN** socket (blue ring) to plug in the other end.

Sneak + right-click a socket to unplug it. Sneak + right-click the air to drop the end you hold.
Each run (socket to clip, clip to clip, clip to socket) reaches 64 to 128 blocks, depending on
the cable (see below). Cables are not used up: one cable item makes any number of connections.

## Walls and obstacles

Cables hang and rest on low obstacles (up to about one block, like a step or a ledge). A cable
that would pass through a wall is refused: add a cable clip to route around it.

## Connectors

Every socket has the connector real equipment has, and a cable only plugs into a socket that
takes its plug. Point the crosshair at a socket to see its name, connector and direction.

| Connector | What it carries | Looks like |
|---|---|---|
| **XLR** | Microphones and balanced line signal | Round, three pins |
| **TRS** (6.35 mm jack) | Line and instrument signal | Small round hole |
| **XLR/TRS combo** | Takes either plug; console and amplifier inputs | Three pins round a jack hole |
| **Speakon** | Speaker level: amplifier outputs and passive speakers only | Wide blue-grey ring |
| **Multipin** | All of a stage box's channels in one plug | Flat, a row of pins |

| Device | Sockets |
|---|---|
| Wireless receiver, stand mic, gooseneck mic, choir mic | OUT: XLR |
| Media deck, video capture | OUT: TRS |
| Stage box | CH 1-8 / 1-16: XLR; SNAKE: multipin |
| Console | IN 1-16: combo; stereo IN: TRS; MAIN L/R: XLR; AUX 1/2: TRS |
| Power amplifier | IN A/B: combo; OUT 1-4: Speakon |
| Graphic EQ, compressor, effects processor, crossover | IN: combo; OUT: XLR |
| DI box | IN: TRS (instrument); OUT: XLR |
| Every passive speaker (line array, PA cabinet, concert stack, wedge, subwoofer) | INPUT: Speakon |
| In-ear transmitter, intercom base station | INPUT / PGM IN: combo |
| Wall outlet | any plug (it is a patch plate) |

## Cable types

Each cable has its plug on both ends, reaches its own distance per run and has its own colour in
the world, so you can tell the lines apart.

| Cable | Plug | Per run | Colour |
|---|---|---|---|
| Audio cable | TRS | 64 blocks | Black |
| XLR mic cable | XLR | 96 blocks | Dark blue |
| Speaker cable | Speakon | 96 blocks | Orange |
| Multicore snake | Multipin; its fan-out goes into console inputs (or a wall outlet) | 128 blocks | Thick black |
| Cable reel | XLR (a long XLR cable on a drum) | 128 blocks | Yellow |

A cable that doesn't fit is refused with a message naming the socket, its connector and the
cable to use. Typical chain: mic or receiver **XLR** → console combo input; console **MAIN XLR** →
amplifier combo input; amplifier **Speakon** OUT → speaker; console **AUX TRS** → in-ear
transmitter or a wedge's amplifier.

## Phantom power (+48V)

Condenser microphones (the gooseneck podium mic and the hanging choir mic) need +48V phantom
power from the console. Press **+48V** in the console's master section (next to the USB player
buttons); without it they are silent. The stand mic is dynamic and works either way, and
wireless receivers don't need it. Phantom power doesn't pass through processors such as a
compressor: plug condenser mics into the console directly, or through stage boxes and outlets.

## Patching tools

- **Stage box (8/16 ch)**: plug mics into it on stage, run one snake to the console.
- **Wall outlet**: four sockets, each in or out; chain outlets or split one signal.
- **DI box**: converts a line source for a mic input.
- **Cable reel**: carries cable.

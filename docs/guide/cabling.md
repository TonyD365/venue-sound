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

## Cable types

Each cable does one job, reaches its own distance per run and has its own colour in the world,
so you can tell the lines apart.

| Cable | Connects | Per run | Colour |
|---|---|---|---|
| Audio cable | Anything | 64 blocks | Black |
| XLR mic cable | Mic and line signal: mics, receivers, players, processors, consoles, powered speakers. Not an amplifier's outputs | 96 blocks | Dark blue |
| Speaker cable | Only an amplifier OUT to a speaker INPUT (through wall outlets if needed) | 96 blocks | Orange |
| Multicore snake | Only a stage box's SNAKE socket to the console (or a wall outlet), carrying all 8 or 16 channels | 128 blocks | Thick black |
| Cable reel | Anything, for long runs across the venue | 128 blocks | Yellow |

A cable that doesn't fit the job is refused with a message saying which one to use.

## Patching tools

- **Stage box (8/16 ch)**: plug mics into it on stage, run one snake to the console.
- **Wall outlet**: four sockets, each in or out; chain outlets or split one signal.
- **DI box**: converts a line source for a mic input.
- **Cable reel**: carries cable.

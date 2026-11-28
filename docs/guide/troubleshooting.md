# Troubleshooting and FAQ

## No sound

Check the chain in order:

1. Simple Voice Chat works (press V; others hear you nearby).
2. Mic channel = receiver channel; you are within 128 blocks of the receiver.
3. Receiver OUT → console IN; that strip's LED is lit.
4. Strip fader up, MUTE off; MAIN L/R faders up.
5. MAIN → amplifier IN A/B; amplifier switched on, level up.
6. Amplifier OUT → speaker INPUT; speaker switched on; you are within its coverage.

## A cable is refused

- "would go through a wall": route it along cable clips.
- "already has a cable": that socket is in use; unplug it with sneak + right-click.
- "too far": each cable has its own reach per run (64–128 blocks, see [cabling](cabling.md#cable-types)); add clips, outlets or a stage box.

## WATERFrAMES screens

Put them in a video capture zone and cable the capture device to the console. Their volume then
follows the console and the speakers' distance. Their sound can't come from the speakers'
direction: WATERFrAMES plays video audio outside Minecraft's sound engine.

## Crashes

Make sure Fabric API and Simple Voice Chat are installed for 1.20.1, then
[open an issue](https://github.com/TonyD365/venue-sound/issues/new/choose) with `logs/latest.log`.

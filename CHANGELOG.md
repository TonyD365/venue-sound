# Changelog

## 0.3.0 (unreleased)

The first public version.

- **Signal chain**: sources → cables plugged socket to socket → mixing console → rack processors →
  power amplifier → speakers, heard by every player from where they stand.
- **Sources**: wireless microphone (16 channels), headset and lavalier mics you wear, wireless
  receiver, stand, gooseneck and hanging choir mics, media deck (mp3/wav), video capture zone for
  WATERFrAMES screens.
- **Consoles**: VS-12 (8 mono + 2 stereo, 3×2 blocks) and VS-24 (16 mono + 4 stereo, 5×2 blocks)
  with gain, peak LED, 3-band EQ, AUX 1/2, effects send, pan, mute, PFL, faders, built-in USB
  player, effects unit and meters. Every control is used on the model, with right-click or the
  mouse wheel. All sockets are on the rear side, facing the stage. Point at a knob or fader to
  read its value; the MAIN meters and PEAK LEDs follow the real signal.
- **Rack**: power amplifier (stereo IN A/B), graphic EQ, compressor/limiter, effects processor,
  crossover, power sequencer, rack cabinet.
- **Speakers**: line array module, flown line array with truss point, PA cabinet, 4×6 concert
  stack, floor wedge, subwoofer, in-ear monitor transmitter and earpiece (16 channels: each
  earpiece hears only the transmitter on its channel, for one monitor mix per performer).
- **Intercom**: base station and crew beltpacks on channel A or B. With TALK on, your voice goes
  straight to the crew on your channel near a powered station; the audience never hears it. The
  station's program input lets the crew follow the show.
- **Cabling**: audio, XLR and speaker cables, multicore snake with 8/16-channel stage boxes, wall
  outlet, DI box, cable clips, cable reel. Cables are drawn in the world, rest on low obstacles and
  can't go through walls; a held cable runs to your hand.
- **Cable types** each do one job and have their own colour and reach per run: audio cable
  (anything, 64 blocks), XLR (mic and line signal, 96), speaker cable (amplifier to speakers, 96,
  orange), multicore snake (stage box to console, 128, thick), cable reel (anything, 128, yellow).
- **Real connectors**: every socket has its real connector (XLR, TRS, XLR/TRS combo, Speakon,
  multipin) and each cable its plug; a cable only goes into a socket that takes it. Condenser
  mics (gooseneck, choir) need the console's **+48V** phantom power. Point at a socket to read
  its connector.
- **OSC remote control**: run the console from TouchOSC, Open Stage Control or any OSC software
  (UDP 9000 on your own computer, X32-style addresses too), with feedback to the app.
- **VenueSound Remote** (`venuesound-remote.jar`): run it next to the game and an iPad or any
  browser on the network becomes the console's control surface (every strip, masters, +48V,
  meters), over OSC. Protected by an access code.
- **Minecraft 1.20.1 and 1.21.1**: one jar for each, built from the same source (Stonecutter).
- **Workbench**: a desktop app with the same devices and signal engine, with sound.

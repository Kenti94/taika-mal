# Taika Mal

Taika Mal adds compact, customizable player and NPC status frames to RuneLite.

## Player frame

- Square portrait rendered from the local player's in-game model.
- Combat and total levels in the lower-left of the portrait.
- Hitpoints and prayer bars, with hitpoints shown slightly larger.

## NPC target frame

- Shows the last NPC you interacted with for up to ten seconds.
- Displays one health bar to the left of the NPC portrait.
- NPC health is shown as a percentage when RuneLite exposes its health bar. Until then, the frame displays `--`.
- A portrait is rendered from the NPC's in-game model by default. An optional setting can download and cache NPC thumbnails from the OSRS Wiki.

## Appearance and position

The player and NPC frames have separate settings for panel color, portrait background, border, bar fill, empty bar, bar border, text, and opacity. Both frames can be moved with RuneLite's Alt+drag overlay controls.

The Wiki portrait option is off by default. Enabling it contacts the OSRS Wiki and sends your IP address to that third-party server; RuneLite displays a warning in the setting.

Portraits are generated from the model currently available in the scene, so equipment and animation can affect the image. RuneLite does not always expose exact NPC hitpoints, so NPC health is not shown as an exact point value.

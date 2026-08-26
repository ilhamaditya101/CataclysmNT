# CataclysmNT v2

Paper 1.20.1 plugin that completely hides the vanilla player nametag and renders a configurable replacement using TextDisplay.

## Features
- LuckPerms prefix + suffix.
- Player name is never rendered by CataclysmNT.
- Hex colors (`#RRGGBB`) and MiniMessage (`<#RRGGBB>`) support.
- PlaceholderAPI support when PlaceholderAPI is installed.
- Transparent/no background by default.
- Automatic nametag render distance based on server view-distance.
- Fixed render distance option.
- Smooth `BOB` and `PULSE` animations.
- Automatic following every tick.
- GitHub Actions build with Java 17.

## PlaceholderAPI
PlaceholderAPI is optional. If installed, any PlaceholderAPI placeholders inside `nametag.format` are resolved for the player.

Example:
```yaml
nametag:
  format: "%prefix% <gray>•</gray> %suffix% <dark_gray>(%player_level%)</dark_gray>"
```

## Animation
```yaml
nametag:
  animation:
    type: BOB # NONE, BOB, PULSE
    speed: 0.08
    amplitude: 0.05
    opacity-amount: 45
```

## Distance
```yaml
nametag:
  distance:
    mode: AUTO
    min-range: 16.0
    max-range: 96.0
```
`AUTO` uses the server view-distance converted to blocks. Use `FIXED` and `range` for an exact limit.

## Build
```bash
gradle build
```
The JAR is generated in `build/libs/`.

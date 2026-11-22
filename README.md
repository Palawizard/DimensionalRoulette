# Dimensional Roulette

A Fabric mod for Minecraft: every time you take damage, you are teleported to a random dimension.

![Example](https://raw.githubusercontent.com/Palawizard/DimensionalRoulette/main/ezgif-4c6e26520299e8.gif)

## Features

- Taking damage teleports you to another dimension, picked at random.
- Admins can set a custom spawn point per dimension.
- Dimensions can be blacklisted so you are never sent there.
- Fire is removed before the teleport, and Slow Falling prevents fall damage on arrival.
- You never land inside a wall, suffocate or fall into the void:
  - bedrock under your feet is replaced with stone so you don't get trapped;
  - if you land inside solid blocks, a 3x3 room is carved out around you;
  - if you land in the air, a floating platform is generated under you.

## Commands

General settings:

- `/dimensionalroulette enable`: turns teleportation on damage on.
- `/dimensionalroulette disable`: turns it off.
- `/dimensionalroulette status`: shows whether it is on and which spawn mode is active.

Spawn mode:

- `/dimensionalroulette settings mode spawn`: you arrive near the spawn point of the dimension.
- `/dimensionalroulette settings mode relative`: you arrive at the same X/Z position as before.

Dimensions:

- `/dimensionalroulette dimensions list`: lists every available dimension.
- `/dimensionalroulette dimensions tp <dimension>`: teleports you to a given dimension.
- `/dimensionalroulette dimensions setspawn <dimension>`: sets the custom spawn point of a dimension.
- `/dimensionalroulette dimensions disable <dimension>`: removes a dimension from the teleport pool.
- `/dimensionalroulette dimensions enable <dimension>`: puts a disabled dimension back in the pool.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/).
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Put `dimensional_roulette-x.x.x.jar` in your `mods/` folder.
4. Start the game.

## Compatibility

Works with Fabric 1.21.1, and with modded dimensions such as Aether, Backrooms and Ceilands. If you find a bug or a mod that doesn't work with it, let me know.

## License

MIT. You can use it, modify it and include it in modpacks.

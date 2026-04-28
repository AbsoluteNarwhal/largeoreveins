# Large Ore Veins

This mod replaces Minecraft's default ore generation with massive, highly customisable ore veins. 
This encourages the player to explore the world and also makes mining more difficult. Available for Neoforge 1.21.1.

## Gameplay

Ore veins are now much more difficult to find. The best strategy is to explore caves and hope to stumble across a vein. 
Certain ores are more common in certain biomes:

- Coal: oceans
- Copper: plains
- Diamond: jungle
- Emerald: mountains
- Iron: forests
- Lapis: deserts
- Gold: badlands
- Redstone: swamps

### Notes

- Ancient debris still generates in the normal way. I thought massive ancient debris veins were a bit too overpowered.
- JEI is supported automatically! Click on ores in JEI to see information about the veins they generate in.
- Finding ores can be quite difficult as veins are often not exposed in caves. Although not required, this mod is more
fun when played with mods that add ways to more easily find ores.

## Configuring ore veins

Large Ore Veins uses [datapacks](https://minecraft.wiki/w/Tutorial:Creating_a_data_pack) for customisation. If you are
making a modpack that uses KubeJS, you can use the `kubejs/data` folder for your vein configurations.

In your datapack, you should put all your vein configuration files in one of the following folders: 

- For adding new veins: `data/<your namespace>/ore_veins`
- For editing existing veins: `data/largeoreveins/ore_veins`

Here is an example of an ore vein configuration file:

```json
{
  "id": "largeoreveins:iron_vein",
  "dimension": "minecraft:overworld",
  "replace_blocks": {
    "minecraft:stone": "minecraft:iron_ore",
    "minecraft:deepslate": "minecraft:deepslate_iron_ore",
    "minecraft:andesite": "minecraft:iron_ore",
    "minecraft:diorite": "minecraft:iron_ore",
    "minecraft:granite": "minecraft:iron_ore",
    "minecraft:tuff": "minecraft:deepslate_iron_ore"
  },
  "maxY": 63,
  "minY": -20,
  "size": 35,
  "type": "VEIN",
  "default_weight": 5,
  "biome_weights": {
    "#minecraft:is_forest": 8
  },
  "enabled": true
}
```

- id: A unique identifier for your vein. For custom veins, put this under your mod(pack)'s namespace, such as 
`myawesomemod:zinc_vein`.
- dimension: The dimension to generate your vein in. This can be any vanilla or modded dimension (although I cannot
guarantee that all modded dimensions are compatible).
- replace_blocks: Map stone blocks to ore blocks. In the example, the vein will generate iron ore in place of stone 
and deepslate iron ore in place of deepslate.
- maxY (optional, default = 60): The highest Y value where your vein will generate.
- minY (optional, default = 0): The lowest Y value where your vein will generate.
- size (optional, default = 25): The 'radius' of the vein. The effect this has depends on the vein type.
- type (optional, default = BLOB): Choose between BLOB, VEIN and PLATE:
  - BLOB is a noisy sphere shape.
  - VEIN is a long lumpy shape that looks similar to a scaled up version of vanilla ore veins.
  - PLATE is a thin, flat circular shape.
- default_weight (optional, default = 1): The default weight in any biome. A higher number makes the vein more common.
- biome_weights (optional): Use this to change the weight only in specific biomes. For example, gold veins have a 
higher weight in badlands biomes. Note that this setting may not work for cave biomes.
- enabled (optional, default = true): Set this to false to disable generation of the vein.

To modify an existing vein, place the vein in `data/largeoreveins/ore_veins` (not your own namespace) and make sure 
the id field matches the existing vein.

You can view more examples in `src/main/resources/data/largeoreveins/ore_veins`.
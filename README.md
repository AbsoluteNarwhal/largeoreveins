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
- Although certainly not required, this mod can be very fun with mods that add quarries installed. After finding an ore
vein, you can use a quarry to excavate it.

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
    "minecraft:deepslate": "minecraft:deepslate_iron_ore"
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
`myawesomemod:zinc_vein`
- dimension: The dimension to generate your vein in.
- replace_blocks: Map stone blocks to ore blocks. In the example, the vein will generate iron ore in place of stone 
and deepslate iron ore in place of deepslate.
- maxY: The highest Y value where your vein will generate.
- minY: The lowest Y value where your vein will generate.
- size: The 'radius' of the vein. The effect this has depends on the vein type.
- type: Choose between BLOB, VEIN and PLATE:
  - BLOB is a noisy sphere shape.
  - VEIN is a long noodle-like shape.
  - PLATE is a thin, flat circular shape.
- default_weight: The default weight in any biome. A higher number makes the vein more common.
- biome_weights: Use this to change the weight only in specific biomes. For example, gold veins use this feature to be
much more common in badlands biomes.
- enabled: Set this to false to disable generation of the vein.

To modify an existing vein, place the vein in `data/largeoreveins/ore_veins` (not your own namespace) and make sure 
the id field matches the existing vein.

You can view more examples in `src/main/resources/data/largeoreveins/ore_veins`.
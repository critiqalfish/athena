package one.txrsp.athena.utils;

import net.minecraft.block.Block;

public class Crops {
    public enum CROP {
        CACTUS,
        CARROT,
        COCOA,
        MELON,
        MUSHROOM,
        WARTS,
        POTATO,
        PUMPKIN,
        CANE,
        WHEAT,
        NONE
    }

    public static CROP getCropForBlock(Block block) {
        return switch (block.getTranslationKey()) {
            case "block.minecraft.cactus" -> CROP.CACTUS;
            case "block.minecraft.carrot" -> CROP.CARROT;
            case "block.minecraft.cocoa" -> CROP.COCOA;
            case "block.minecraft.melon" -> CROP.MELON;
            case "block.minecraft.red_mushroom", "block.minecraft.brown_mushroom" -> CROP.MUSHROOM;
            case "block.minecraft.nether_warts" -> CROP.WARTS;
            case "block.minecraft.potatoes" -> CROP.POTATO;
            case "block.minecraft.pumpkin" -> CROP.PUMPKIN;
            case "block.minecraft.sugar_cane" -> CROP.CANE;
            case "block.minecraft.wheat" -> CROP.WHEAT;
            default -> CROP.NONE;
        };
    }

    public static String getToolForCrop(CROP crop) {
        return switch (crop) {
            case CACTUS -> "Cactus Knife";
            case CARROT -> "Gauss Carrot Hoe";
            case COCOA -> "Cocoa Chopper";
            case MELON -> "Melon Dicer";
            case MUSHROOM -> "Fungi Cutter";
            case WARTS -> "Newton Nether Warts Hoe";
            case POTATO -> "Pythagorean Potato Hoe";
            case PUMPKIN -> "Pumpkin Dicer";
            case CANE -> "Turing Sugar Cane Hoe";
            case WHEAT -> "Euclid's Wheat Hoe";
            case NONE -> "";
        };
    }

    public static CROP getCropForTool(String toolName) {
        if (toolName.contains("Cactus Knife")) {
            return CROP.CACTUS;
        } else if (toolName.contains("Gauss Carrot Hoe")) {
            return CROP.CARROT;
        } else if (toolName.contains("Cocoa Chopper")) {
            return CROP.COCOA;
        } else if (toolName.contains("Melon Dicer")) {
            return CROP.MELON;
        } else if (toolName.contains("Fungi Cutter")) {
            return CROP.MUSHROOM;
        } else if (toolName.contains("Newton Nether Warts Hoe")) {
            return CROP.WARTS;
        } else if (toolName.contains("Pythagorean Potato Hoe")) {
            return CROP.POTATO;
        } else if (toolName.contains("Pumpkin Dicer")) {
            return CROP.PUMPKIN;
        } else if (toolName.contains("Turing Sugar Cane Hoe")) {
            return CROP.CANE;
        } else if (toolName.contains("Euclid's Wheat Hoe")) {
            return CROP.WHEAT;
        } else {
            return CROP.NONE;
        }
    }
}

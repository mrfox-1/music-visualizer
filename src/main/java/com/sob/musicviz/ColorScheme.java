package com.sob.musicviz;

import java.awt.Color;

public enum ColorScheme
{
    RAINBOW("Rainbow"),
    OCEAN("Ocean", 0x165DCC, 0x168AAD, 0x34C9C3, 0x90E0EF),
    SUNSET("Sunset", 0xFF6542, 0xFFB14E, 0xE85D9E, 0x9146C7),
    AURORA("Aurora", 0x37D996, 0x38C8D9, 0x7971E8, 0xBF68DE),
    EMBER("Ember", 0xB92D32, 0xE85A28, 0xF39B32, 0xFFD166),
    PASTEL("Pastel", 0xF4A7C1, 0xC4B5FD, 0xA7D8F0, 0xB8E0C4),
    FOREST("Forest", 0x26734D, 0x4D9964, 0x8DB85B, 0xD3C67A),
    ICE("Ice", 0x4478BD, 0x78BCE8, 0xB9E9F2, 0xE7F5FF),
    NEON("Neon", 0xFF38B8, 0x9D4DFF, 0x22DDEE, 0xC6FF38),
    AUTUMN("Autumn", 0xB74135, 0xD97532, 0xE9B949, 0x8A943C),
    ROSE_GOLD("Rose gold", 0xB76E79, 0xE4A6A0, 0xF2CC8F),
    JEWEL_TONES("Jewel tones", 0xC53D68, 0x8753BE, 0x367AC3, 0x32A883),
    AMETHYST("Amethyst", 0x633B91, 0x8D5DB5, 0xB18AD2, 0xD8BCEB),
    EMERALD("Emerald", 0x246B54, 0x359879, 0x64BE98, 0xA3DAB8),
    SAPPHIRE("Sapphire", 0x294891, 0x3C68BD, 0x6896DC, 0xA4C7EF),
    RUBY("Ruby", 0x8E284D, 0xBA3F65, 0xDB7089, 0xF0A6B6),
    EARTH_TONES("Earth tones", 0x92735A, 0xB59A72, 0x8F9E75, 0xCFBC98),
    DESERT("Desert", 0xBC7656, 0xDAA06D, 0xEAC891, 0x83A6A0),
    MOSS_STONE("Moss and stone", 0x75866A, 0xA2AD87, 0x8F999B, 0xBDC4B6),
    COPPER_TEAL("Copper and teal", 0xB87346, 0xDFA778, 0x368E91, 0x78B8B0),
    RETRO_ARCADE("Retro arcade", 0xF05295, 0x825CDD, 0x39BDCD, 0xF1C65A),
    SYNTHWAVE("Synthwave", 0x6653C7, 0xB74BC7, 0xF15B9E, 0xF5A464),
    VAPORWAVE("Vaporwave", 0xE8A4D6, 0xBAA7EC, 0x94CBEB, 0x8CDED1),
    VINTAGE("Vintage", 0xBD725D, 0xD4AF69, 0x8FA98A, 0x6F9297),
    CHERRY_BLOSSOM("Cherry blossom", 0xBB6F91, 0xD894AE, 0xEBB8CA, 0xF5D8E1),
    LAVENDER_MINT("Lavender and mint", 0xA195C9, 0xC6B9E3, 0x94C7BB, 0xBDE2D3),
    PEACH_CREAM("Peach and cream", 0xD8987F, 0xEBB59A, 0xF3D2B6, 0xF6E7CE),
    TWILIGHT("Twilight", 0x526994, 0x8074AC, 0xB18CB8, 0xD4A9B6),
    MOONLIGHT("Moonlight", 0x70859D, 0x96A9BC, 0xBCCBD6, 0xDEE6E9),
    CITRUS("Citrus", 0xEAA444, 0xE7CC5C, 0xB5CA64, 0x7EBB72),
    TROPICAL("Tropical", 0x36B5A2, 0x70CFB1, 0xF1C45C, 0xEE8770),
    BLUE_ORANGE("Complementary: blue / orange", 0x3485E4, 0xF59A38),
    PURPLE_YELLOW("Complementary: purple / yellow", 0x9C62D6, 0xF2D74E),
    RED_GREEN("Complementary: red / green", 0xE45151, 0x52BA79),
    TEAL_CORAL("Complementary: teal / coral", 0x2CBDB2, 0xFF7F70),
    RED_WHITE_BLUE("Flag: red / white / blue", 0xE14A4A, 0xF0F0F0, 0x3C70D8),
    BLUE_YELLOW("Flag: Ukraine", 0x0057B7, 0xFFD700),
    GREEN_WHITE_ORANGE("Flag: Ireland", 0x169B62, 0xFFFFFF, 0xFF883E),
    GREEN_WHITE_RED("Flag: Italy", 0x009246, 0xFFFFFF, 0xCE2B37),
    BLUE_WHITE_RED("Flag: France", 0x0055A4, 0xFFFFFF, 0xEF4135),
    RED_WHITE("Flag: Canada / Japan", 0xE53945, 0xFFFFFF),
    BRAZIL("Flag: Brazil", 0x009B3A, 0xFFDF00, 0x3155A4, 0xFFFFFF),
    PRIDE("Flag: rainbow pride", 0xE84A4A, 0xFF9638, 0xF4D84A, 0x45B96F, 0x477BDD, 0x9D54CD),
    TRANS_PRIDE("Flag: trans pride", 0x5BCEFA, 0xF5A9B8, 0xFFFFFF),
    BI_PRIDE("Flag: bi pride", 0xD60270, 0x9B4F96, 0x3560CE),
    PAN_PRIDE("Flag: pan pride", 0xFF218C, 0xFFD800, 0x21B1FF),
    MONOCHROME("Monochrome", 0xB8C4D0),
    CUSTOM("Custom gradient");

    private final String label;
    private final Color[] colors;

    ColorScheme(String label, int... rgb)
    {
        this.label = label;
        colors = new Color[rgb.length];
        for (int i = 0; i < rgb.length; i++) colors[i] = new Color(rgb[i]);
    }

    Color forNote(int note, Color custom, Color customEnd)
    {
        if (this == RAINBOW) return NoteColor.forNote(note);
        if (this == CUSTOM)
        {
            float blend = Math.floorMod(note, 12) / 11f;
            return new Color(
                Math.round(custom.getRed() + (customEnd.getRed() - custom.getRed()) * blend),
                Math.round(custom.getGreen() + (customEnd.getGreen() - custom.getGreen()) * blend),
                Math.round(custom.getBlue() + (customEnd.getBlue() - custom.getBlue()) * blend));
        }
        return colors[Math.floorMod(note, 12) * colors.length / 12];
    }

    @Override public String toString() { return label; }
}

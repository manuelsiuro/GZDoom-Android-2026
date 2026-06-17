// Host-side diagnostic for TextureReader: open an IWAD and decode a few known
// textures/flats/sprites, printing dimensions + an opaque-pixel count so the
// composite-texture / patch / flat decoders can be validated on real data
// without a device. Build via tools/build_texture_probe.sh.
//
//   ./texture_probe /path/to/freedoom2.wad
#include "../TextureReader.h"
#include <cstdio>
#include <string>
#include <vector>

using png2wad::TextureReader;
using png2wad::DecodedImage;

static int opaqueCount(const DecodedImage& img) {
    int n = 0;
    for (uint32_t p : img.argb) if ((p >> 24) != 0) n++;
    return n;
}

static void probe(TextureReader& r, const char* name) {
    DecodedImage img = r.GetImage(name);
    if (!img.valid()) {
        printf("  %-10s : (not decodable)\n", name);
        return;
    }
    printf("  %-10s : %dx%d, %d opaque px\n", name, img.width, img.height, opaqueCount(img));
}

int main(int argc, char** argv) {
    if (argc < 2) {
        fprintf(stderr, "usage: %s <iwad.wad>\n", argv[0]);
        return 2;
    }
    TextureReader reader;
    if (!reader.Open(argv[1], {})) {
        fprintf(stderr, "FAIL: could not open '%s' (no palette?)\n", argv[1]);
        return 1;
    }
    printf("walls=%zu flats=%zu\n", reader.WallNames().size(), reader.FlatNames().size());

    // A few classic Doom2/Freedoom names (composite wall, flats, a monster sprite).
    printf("samples:\n");
    probe(reader, "STARTAN2");
    probe(reader, "BRICK1");
    probe(reader, "FLAT1");
    probe(reader, "FLOOR0_1");
    probe(reader, "TROOA1");

    // First few of each list, to confirm enumeration + decode of real entries.
    printf("first walls: ");
    for (size_t i = 0; i < reader.WallNames().size() && i < 5; i++) printf("%s ", reader.WallNames()[i].c_str());
    printf("\nfirst flats: ");
    for (size_t i = 0; i < reader.FlatNames().size() && i < 5; i++) printf("%s ", reader.FlatNames()[i].c_str());
    printf("\n");

    bool ok = reader.WallNames().size() > 50 && reader.FlatNames().size() > 20 &&
              reader.GetImage("FLAT1").valid();
    printf("%s\n", ok ? "PROBE OK" : "PROBE WEAK (unexpectedly few textures)");
    return ok ? 0 : 1;
}

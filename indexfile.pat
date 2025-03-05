#include <std/string.pat>
#include <std/core.pat>
#pragma pattern_limit 0x1000000
#pragma endian big

struct JavaString {
    u16 bytes;
    char content[bytes];
};
struct IndexEntry {
    u32 confirmation; // 4957
    // this only reads the `0 version
    JavaString versionS;
    JavaString filename;
    s64 time;
    s64 size;
} [[format_read("entry_formatter")]];
fn entry_formatter(ref auto e) {
    return std::format("{} [{},{}]", e.filename.content, e.time, e.size);
};

struct IndexFile {
    JavaString hostname;
    // fill to taste
    IndexEntry entries[1784];
    u32 footer; // 0
};

IndexFile file @ 0;

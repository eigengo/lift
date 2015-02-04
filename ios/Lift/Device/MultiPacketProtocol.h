#ifndef __Lift__MultiPacketProtocol__
#define __Lift__MultiPacketProtocol__

#include <stdint.h>

/*
 
 header: UInt16     = 0xcab1 // + 2 B
 count: Byte        = ...    // + 3 B
 timestamp: UInt32  = ...    // + 7 B
 ===
 size0: UInt16      = ...    // + 9 B
 sloc0: Byte        = ...    // + 10 B
 data0: Array[Byte] = ...
 size1: UInt16      = ...
 sloc1: Byte        = ...
 data1: Array[Byte] = ...
 ...
 sizen: UInt16      = ...
 slocn: Byte        = ...
 datan: Array[Byte] = ...
 */

/**
 * The header is a packed structure written as big-endian encoded values
 */
typedef struct __attribute__((__packed__)) {
    uint16_t header;        // 0xcab1
    uint8_t  count;         // number of entries
    uint32_t timestamp;     // timestamp (some unitless, but monotonously increasing number)
} MultiPacketHeader;

/**
 * The header that preceeds every entry in the multi packet
 */
typedef struct __attribute__((__packed__)) {
    uint16_t size;
    uint8_t  sloc;
    // variable data follows
} MultiPacketEntry;

#endif /* defined(__Lift__MultiPacketProtocol__) */

#pragma once
extern "C" {
#include <pebble.h>
}
#include "accel_service.h"

namespace pebble {
namespace mock {

    class Pebble {
    public:
        static AccelService accelService;
    };


}
}
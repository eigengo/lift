#pragma once
#include <vector>

extern "C" {
#include <pebble.h>
}

namespace pebble {
namespace mock {

    class AccelService {
    private:
        std::vector<AccelRawData> raw_data;
        int samples_per_update;
        AccelRawDataHandler raw_data_handler;
    public:
        AccelService& operator<<(const AccelRawData data);
        AccelService& operator<<(const std::vector<AccelRawData> &data);

        void accel_raw_data_service_subscribe(uint32_t samples_per_update, AccelRawDataHandler handler);

        void trigger_raw();
    };

}
}

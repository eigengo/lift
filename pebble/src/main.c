#include "pebble.h"
#include "../core/main/gfs.h"
#include "../core/main/am.h"

#define TIMER_MS 1000

static Window *window;

static GRect window_frame;

static Layer *disc_layer;

static AppTimer *timer;

#define TEXT_LENGTH 150

static void disc_layer_update_callback(Layer *me, GContext *ctx) {
    graphics_context_set_text_color(ctx, GColorWhite);
    GRect bounds = layer_get_frame(me);

    char text[TEXT_LENGTH];

    snprintf(text, TEXT_LENGTH - 1, "Tag: %lx\nLE: %d %s\nLED: %d\nEC: %d\nP: %d\nQueue: %d\nUB: %d",
    	am_tag(), am_last_error(), am_last_error_text(), am_last_error_distance(), am_error_count(), am_count(), am_queue_length(), heap_bytes_used());

    graphics_draw_text(ctx,
            text,
            fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD),
            GRect(5, 5, bounds.size.w - 10, 100),
            GTextOverflowModeWordWrap,
            GTextAlignmentLeft,
            NULL);
}

static void timer_callback(void *data) {
    layer_mark_dirty(disc_layer);
    timer = app_timer_register(TIMER_MS, timer_callback, NULL);
}

static void window_load(Window *window) {
    Layer *window_layer = window_get_root_layer(window);
    GRect frame = window_frame = layer_get_frame(window_layer);

    disc_layer = layer_create(frame);
    layer_set_update_proc(disc_layer, disc_layer_update_callback);
    layer_add_child(window_layer, disc_layer);
}

static void window_unload(Window *window) {
    am_stop();
    layer_destroy(disc_layer);
}

static void init(void) {
    srand(time(NULL));
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers) {
            .load = window_load,
            .unload = window_unload
    });
    window_stack_push(window, true /* Animated */);
    window_set_background_color(window, GColorBlack);

    gfs_start(am_start(), GFS_SAMPLING_100HZ);

    timer = app_timer_register(TIMER_MS, timer_callback, NULL);
}

static void deinit(void) {
    gfs_stop();
    am_stop();

    window_destroy(window);
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}

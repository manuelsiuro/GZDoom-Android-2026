
LOCAL_PATH := $(call my-dir)/../libraries/abseil

include $(CLEAR_VARS)

LOCAL_MODULE := abseil_uz

LOCAL_CPPFLAGS := -std=c++17 -fexceptions -frtti
LOCAL_CFLAGS   := -fvisibility=hidden -fdata-sections -ffunction-sections \
                  -DABSL_OPTION_USE_STD_RANGES=0

LOCAL_C_INCLUDES := $(LOCAL_PATH)

LOCAL_SRC_FILES = \
    absl/base/log_severity.cc \
    absl/base/internal/cycleclock.cc \
    absl/base/internal/low_level_alloc.cc \
    absl/base/internal/poison.cc \
    absl/base/internal/raw_logging.cc \
    absl/base/internal/spinlock.cc \
    absl/base/internal/spinlock_wait.cc \
    absl/base/internal/strerror.cc \
    absl/base/internal/sysinfo.cc \
    absl/base/internal/thread_identity.cc \
    absl/base/internal/tracing.cc \
    absl/base/internal/unscaledcycleclock.cc \
    absl/base/throw_delegate.cc \
    \
    absl/container/internal/hashtablez_sampler.cc \
    absl/container/internal/hashtablez_sampler_force_weak_definition.cc \
    absl/container/internal/raw_hash_set.cc \
    \
    absl/crc/crc32c.cc \
    absl/crc/internal/cpu_detect.cc \
    absl/crc/internal/crc.cc \
    absl/crc/internal/crc_cord_state.cc \
    absl/crc/internal/crc_memcpy_fallback.cc \
    absl/crc/internal/crc_memcpy_x86_arm_combined.cc \
    absl/crc/internal/crc_non_temporal_memcpy.cc \
    absl/crc/internal/crc_x86_arm_combined.cc \
    \
    absl/debugging/internal/borrowed_fixup_buffer.cc \
    absl/debugging/internal/examine_stack.cc \
    absl/debugging/stacktrace.cc \
    \
    absl/hash/internal/city.cc \
    absl/hash/internal/hash.cc \
    \
    absl/numeric/int128.cc \
    \
    absl/profiling/internal/exponential_biased.cc \
    absl/profiling/internal/periodic_sampler.cc \
    \
    absl/strings/ascii.cc \
    absl/strings/charconv.cc \
    absl/strings/cord.cc \
    absl/strings/cord_analysis.cc \
    absl/strings/escaping.cc \
    absl/strings/internal/charconv_bigint.cc \
    absl/strings/internal/charconv_parse.cc \
    absl/strings/internal/cord_internal.cc \
    absl/strings/internal/cord_rep_btree.cc \
    absl/strings/internal/cord_rep_btree_navigator.cc \
    absl/strings/internal/cord_rep_btree_reader.cc \
    absl/strings/internal/cord_rep_consume.cc \
    absl/strings/internal/cord_rep_crc.cc \
    absl/strings/internal/cordz_functions.cc \
    absl/strings/internal/cordz_handle.cc \
    absl/strings/internal/cordz_info.cc \
    absl/strings/internal/cordz_sample_token.cc \
    absl/strings/internal/damerau_levenshtein_distance.cc \
    absl/strings/internal/escaping.cc \
    absl/strings/internal/generic_printer.cc \
    absl/strings/internal/memutil.cc \
    absl/strings/internal/ostringstream.cc \
    absl/strings/internal/pow10_helper.cc \
    absl/strings/internal/str_format/arg.cc \
    absl/strings/internal/str_format/bind.cc \
    absl/strings/internal/str_format/extension.cc \
    absl/strings/internal/str_format/float_conversion.cc \
    absl/strings/internal/str_format/output.cc \
    absl/strings/internal/str_format/parser.cc \
    absl/strings/internal/stringify_sink.cc \
    absl/strings/internal/utf8.cc \
    absl/strings/match.cc \
    absl/strings/numbers.cc \
    absl/strings/str_cat.cc \
    absl/strings/str_replace.cc \
    absl/strings/str_split.cc \
    absl/strings/substitute.cc \
    \
    absl/synchronization/barrier.cc \
    absl/synchronization/blocking_counter.cc \
    absl/synchronization/internal/create_thread_identity.cc \
    absl/synchronization/internal/futex_waiter.cc \
    absl/synchronization/internal/graphcycles.cc \
    absl/synchronization/internal/kernel_timeout.cc \
    absl/synchronization/internal/per_thread_sem.cc \
    absl/synchronization/internal/pthread_waiter.cc \
    absl/synchronization/internal/sem_waiter.cc \
    absl/synchronization/internal/stdcpp_waiter.cc \
    absl/synchronization/internal/waiter_base.cc \
    absl/synchronization/mutex.cc \
    absl/synchronization/notification.cc \
    \
    absl/time/civil_time.cc \
    absl/time/clock.cc \
    absl/time/duration.cc \
    absl/time/format.cc \
    absl/time/internal/cctz/src/civil_time_detail.cc \
    absl/time/internal/cctz/src/time_zone_fixed.cc \
    absl/time/internal/cctz/src/time_zone_format.cc \
    absl/time/internal/cctz/src/time_zone_if.cc \
    absl/time/internal/cctz/src/time_zone_impl.cc \
    absl/time/internal/cctz/src/time_zone_info.cc \
    absl/time/internal/cctz/src/time_zone_libc.cc \
    absl/time/internal/cctz/src/time_zone_lookup.cc \
    absl/time/internal/cctz/src/time_zone_posix.cc \
    absl/time/internal/cctz/src/zone_info_source.cc \
    absl/time/time.cc \

include $(BUILD_STATIC_LIBRARY)

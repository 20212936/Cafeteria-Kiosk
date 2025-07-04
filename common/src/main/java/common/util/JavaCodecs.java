package common.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class JavaCodecs {
    public static final Codec<Path> PATH = Codec.STRING.xmap(Path::of, Path::toString);
    public static final Codec<Byte[]> BYTE_ARRAY = Codec.BYTE.listOf().xmap(bytes -> bytes.toArray(new Byte[0]), Arrays::asList);
    public static final Codec<LocalDateTime> LOCAL_DATE_TIME = Codec.STRING.xmap(LocalDateTime::parse, LocalDateTime::toString);

    public static <E> Codec<E> orCompressed(Codec<E> uncompressedCodec, Codec<E> compressedCodec) {
        return new Codec<E>() {
            @Override
            public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
                return ops.compressMaps() ? compressedCodec.encode(input, ops, prefix) : uncompressedCodec.encode(input, ops, prefix);
            }

            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
                return ops.compressMaps() ? compressedCodec.decode(ops, input) : uncompressedCodec.decode(ops, input);
            }

            public String toString() {
                return uncompressedCodec + " orCompressed " + compressedCodec;
            }
        };
    }

    private static Codec<Integer> rangedInt(int min, int max, Function<Integer, String> messageFactory) {
        return Codec.INT
                .validate(
                        value -> value.compareTo(min) >= 0 && value.compareTo(max) <= 0 ? DataResult.success(value) : DataResult.error(() -> messageFactory.apply(value))
                );
    }

    public static Codec<Integer> rangedInt(int min, int max) {
        return rangedInt(min, max, value -> "Value must be within range [" + min + ";" + max + "]: " + value);
    }

    public static <E> Codec<E> rawIdChecked(ToIntFunction<E> elementToRawId, IntFunction<E> rawIdToElement, int errorRawId) {
        return Codec.INT
                .flatXmap(
                        rawId -> Optional.ofNullable(rawIdToElement.apply(rawId))
                                .map(DataResult::success)
                                .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + rawId)),
                        element -> {
                            int j = elementToRawId.applyAsInt(element);
                            return j == errorRawId ? DataResult.error(() -> "Element with unknown id: " + element) : DataResult.success(j);
                        }
                );
    }


    public static byte[] asByteArray(Byte[] bytes) {
        byte[] byteArray = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byteArray[i] = bytes[i];
        }

        return byteArray;
    }
    public static Byte[] asBoxingByteArray(byte[] bytes) {
        Byte[] byteArray = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byteArray[i] = bytes[i];
        }

        return byteArray;
    }
}


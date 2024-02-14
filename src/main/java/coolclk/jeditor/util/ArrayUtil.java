package coolclk.jeditor.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Array 类工具
 * @author CoolCLK
 */
public class ArrayUtil {
    /**
     * 合并数组。
     * @param arrays 要连接的数组
     * @return 按参数顺序连接数组合并为一个<strong>新的<strong/>数组
     * @author CoolCLK
     */
    public static <T> List<T> connect(List<List<T>> arrays) {
        List<T> array = new ArrayList<>();
        for (List<T> a : arrays) {
            array.addAll(a);
        }
        return array;
    }

    /**
     * 合并数组。
     * @param arrays 要连接的数组
     * @return 按参数顺序连接数组合并为一个<strong>新的<strong/>数组
     * @author CoolCLK
     */
    @SafeVarargs
    public static <T> List<T> connect(List<T>... arrays) {
        return connect(Arrays.asList(arrays));
    }

    /**
     * 合并数组。
     * @param arrays 要连接的数组
     * @return 按参数顺序连接数组合并为一个<strong>新的</strong>数组.
     * @apiNote 此方法使用时会强转一个 Object[] 为一个 T[] , 实际类型为 T[]
     * @author CoolCLK
     */
    @SafeVarargs
    @SuppressWarnings({ "unchecked" })
    public static <T> T[] connect(T[]... arrays) {
        List<T>[] arrayLists = new ArrayList[arrays.length];
        for (int i = 0; i < arrayLists.length; i++) {
            arrayLists[i] = new ArrayList<>();
            arrayLists[i].addAll(Arrays.asList(arrays[i]));
        }
        List<T> conventedResult = connect(arrayLists);
        return conventedResult.toArray((T[]) Array.newInstance(conventedResult.get(0).getClass(), 0));
    }

    /**
     * 合并数组。
     * @param arrays 要连接的数组 <i>(请保证此参数长度大于 0)</i>
     * @return 按参数顺序连接数组合并为一个<strong>新的<strong/>数组
     * @author CoolCLK
     */
    public static byte[] connect(byte[]... arrays) {
        if (arrays.length == 0) {
            return new byte[0];
        }
        Byte[][] convertedArrays = new Byte[arrays.length][];
        for (int i = 0; i < convertedArrays.length; i++) {
            convertedArrays[i] = StreamUtil.bytesToByteArray(arrays[i]);
        }
        Byte[] bArray = ArrayUtil.<Byte>connect(convertedArrays);
        return StreamUtil.byteArrayToBytes(bArray);
    }

    /**
     * 获得一个重复单个数据的数组
     * @author CoolCLK
     */
    public static <T> List<T> repeat(T object, int length) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            list.add(object);
        }
        return list;
    }

    /**
     * 数组是否以输入参数正序结尾
     * @author CoolCLK
     */
    public static <T> boolean endsWith(T[] array, T[] objects) {
        if (array.length < objects.length) {
            return false;
        }
        int o = 0;
        for (int i = array.length - objects.length; i < array.length; i++) {
            if (array[i] != objects[o]) {
                return false;
            }
            o++;
        }
        return true;
    }

    /**
     * 数组是否以输入参数正序结尾
     * @author CoolCLK
     */
    public static <T> boolean endsWith(List<T> array, T[] objects) {
        return endsWith(array.toArray(), objects);
    }
}

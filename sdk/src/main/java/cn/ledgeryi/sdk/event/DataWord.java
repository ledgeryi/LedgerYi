package cn.ledgeryi.sdk.event;

import cn.ledgeryi.sdk.common.utils.ByteUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class DataWord implements Comparable<DataWord> {

  // Maximum value of the DataWord
  public static final int WORD_SIZE = 32;
  public static final int MAX_POW = 256;
  public static final BigInteger _2_256 = BigInteger.valueOf(2).pow(256);
  public static final BigInteger MAX_VALUE = _2_256.subtract(BigInteger.ONE);
  public static final DataWord ZERO = new DataWord(new byte[WORD_SIZE]);
  public static final DataWord ONE = DataWord.of((byte) 1);
  private byte[] data = new byte[32];

  public DataWord() {
  }

  public DataWord(int num) {
    this(ByteBuffer.allocate(4).putInt(num));
  }

  public DataWord(long num) {
    this(ByteBuffer.allocate(8).putLong(num));
  }

  private DataWord(ByteBuffer buffer) {
    final ByteBuffer targetByteBuffer = ByteBuffer.allocate(WORD_SIZE);
    final byte[] array = buffer.array();
    System.arraycopy(array, 0, targetByteBuffer.array(), WORD_SIZE - array.length, array.length);
    this.data = targetByteBuffer.array();
  }

  @JsonCreator
  public DataWord(String data) {
    this(Hex.decode(data));
  }

  public DataWord(byte[] data) {
    if (data == null) {
      this.data = ByteUtil.EMPTY_BYTE_ARRAY;
    } else if (data.length == WORD_SIZE) {
      this.data = data;
    } else if (data.length < WORD_SIZE) {
      System.arraycopy(data, 0, this.data, WORD_SIZE - data.length, data.length);
    } else {
      throw new RuntimeException("Data word can't exceed 32 bytes: " + ByteUtil.toHexString(data));
    }
  }

  public static DataWord ONE() {
    return DataWord.of((byte) 1);
  }

  public static DataWord ZERO() {
    return new DataWord(new byte[32]);
  }

  public static DataWord of(byte num) {
    byte[] bb = new byte[WORD_SIZE];
    bb[31] = num;
    return new DataWord(bb);
  }

  @JsonCreator
  public static DataWord of(String data) {
    return of(Hex.decode(data));
  }

  public static DataWord of(byte[] data) {
    if (data == null || data.length == 0) {
      return DataWord.ZERO;
    }

    int leadingZeroBits = numberOfLeadingZeros(data);
    int valueBits = 8 * data.length - leadingZeroBits;
    if (valueBits <= 8) {
      if (data[data.length - 1] == 0) return DataWord.ZERO;
      if (data[data.length - 1] == 1) return DataWord.ONE;
    }

    if (data.length == 32)
      return new DataWord(java.util.Arrays.copyOf(data, data.length));
    else if (data.length <= 32) {
      byte[] bytes = new byte[32];
      System.arraycopy(data, 0, bytes, 32 - data.length, data.length);
      return new DataWord(bytes);
    } else {
      throw new RuntimeException(String.format("Data word can't exceed 32 bytes: 0x%s", ByteUtil.toHexString(data)));
    }
  }

  /**
   * Returns a number of zero bits preceding the highest-order ("leftmost") one-bit
   * interpreting input array as a big-endian integer value
   */
  public static int numberOfLeadingZeros(byte[] bytes) {

    int i = firstNonZeroByte(bytes);

    if (i == -1) {
      return bytes.length * 8;
    } else {
      int byteLeadingZeros = Integer.numberOfLeadingZeros((int)bytes[i] & 0xff) - 24;
      return i * 8 + byteLeadingZeros;
    }
  }

  public static int firstNonZeroByte(byte[] data) {
    for (int i = 0; i < data.length; ++i) {
      if (data[i] != 0) {
        return i;
      }
    }
    return -1;
  }

  public static String bigIntValue(byte[] data) {
    return new BigInteger(data).toString();
  }

  public static boolean isZero(byte[] data) {
    for (byte tmp : data) {
      if (tmp != 0) {
        return false;
      }
    }
    return true;
  }

  public static String shortHex(byte[] data) {
    byte[] bytes = ByteUtil.stripLeadingZeroes(data);
    String hexValue = Hex.toHexString(bytes).toUpperCase();
    return "0x" + hexValue.replaceFirst("^0+(?!$)", "");
  }

  public static long sizeInWords(long bytesSize) {
    return bytesSize == 0 ? 0 : (bytesSize - 1) / WORD_SIZE + 1;
  }

  public static DataWord[] parseArray(byte[] data) {
    int len = data.length / WORD_SIZE;
    DataWord[] words = new DataWord[len];
    for (int i = 0; i < len; i++) {
      byte[] bytes = Arrays.copyOfRange(data, i * WORD_SIZE, (i + 1) * WORD_SIZE);
      words[i] = new DataWord(bytes);
    }
    return words;
  }

  public static boolean equalAddressByteArray(byte[] arr1, byte[] arr2) {
    if (arr1 == arr2) {
      return true;
    }
    if (arr1 == null || arr2 == null || arr1.length < 20 || arr2.length < 20) {
      return false;
    }

    int i = arr1.length - 20;
    int j = arr2.length - 20;

    for (; i < arr1.length && j < arr2.length; i++, j++) {
      if (arr1[i] != arr2[j]) {
        return false;
      }
    }
    return true;
  }

  public byte[] getData() {
    return data;
  }

  /**
   * be careful, this one will not throw Exception when data.length > WORD_SIZE
   */
  public byte[] getClonedData() {
    byte[] ret = ByteUtil.EMPTY_BYTE_ARRAY;
    if (data != null) {
      ret = new byte[WORD_SIZE];
      int dataSize = Math.min(data.length, WORD_SIZE);
      System.arraycopy(data, 0, ret, 0, dataSize);
    }
    return ret;
  }

  public byte[] getNoLeadZeroesData() {
    return ByteUtil.stripLeadingZeroes(data);
  }

  public byte[] getLast20Bytes() {
    return Arrays.copyOfRange(data, 12, data.length);
  }

  public BigInteger value() {
    return new BigInteger(1, data);
  }

  /**
   * Converts this DataWord to an int, checking for lost information. If this DataWord is out of the
   * possible range for an int result then an ArithmeticException is thrown.
   *
   * @return this DataWord converted to an int.
   * @throws ArithmeticException - if this will not fit in an int.
   */
  public int intValue() {
    int intVal = 0;

    for (byte aData : data) {
      intVal = (intVal << 8) + (aData & 0xff);
    }

    return intVal;
  }

  /**
   * In case of int overflow returns Integer.MAX_VALUE otherwise works as #intValue()
   */
  public int intValueSafe() {
    int bytesOccupied = bytesOccupied();
    int intValue = intValue();
    if (bytesOccupied > 4 || intValue < 0) {
      return Integer.MAX_VALUE;
    }
    return intValue;
  }

  /**
   * Converts this DataWord to a long, checking for lost information. If this DataWord is out of the
   * possible range for a long result then an ArithmeticException is thrown.
   *
   * @return this DataWord converted to a long.
   * @throws ArithmeticException - if this will not fit in a long.
   */
  public long longValue() {

    long longVal = 0;
    for (byte aData : data) {
      longVal = (longVal << 8) + (aData & 0xff);
    }

    return longVal;
  }

  /**
   * In case of long overflow returns Long.MAX_VALUE otherwise works as #longValue()
   */
  public long longValueSafe() {
    int bytesOccupied = bytesOccupied();
    long longValue = longValue();
    if (bytesOccupied > 8 || longValue < 0) {
      return Long.MAX_VALUE;
    }
    return longValue;
  }

  public BigInteger sValue() {
    return new BigInteger(data);
  }

  public String bigIntValue() {
    return new BigInteger(data).toString();
  }

  public boolean isZero() {
    for (byte tmp : data) {
      if (tmp != 0) {
        return false;
      }
    }
    return true;
  }

  // only in case of signed operation
  // when the number is explicit defined
  // as negative
  public boolean isNegative() {
    int result = data[0] & 0x80;
    return result == 0x80;
  }

  public DataWord and(DataWord w2) {

    for (int i = 0; i < this.data.length; ++i) {
      this.data[i] &= w2.data[i];
    }
    return this;
  }

  public DataWord or(DataWord w2) {

    for (int i = 0; i < this.data.length; ++i) {
      this.data[i] |= w2.data[i];
    }
    return this;
  }

  public DataWord xor(DataWord w2) {

    for (int i = 0; i < this.data.length; ++i) {
      this.data[i] ^= w2.data[i];
    }
    return this;
  }

  public void negate() {
    if (this.isZero()) {
      return;
    }

    bnot();
    add(DataWord.ONE());
  }

  public void bnot() {
    if (this.isZero()) {
      this.data = ByteUtil.copyToArray(MAX_VALUE);
      return;
    }
    this.data = ByteUtil.copyToArray(MAX_VALUE.subtract(this.value()));
  }

  // By   : Holger
  // From : http://stackoverflow.com/a/24023466/459349
  public void add(DataWord word) {
    byte[] result = new byte[32];
    for (int i = 31, overflow = 0; i >= 0; i--) {
      int v = (this.data[i] & 0xff) + (word.data[i] & 0xff) + overflow;
      result[i] = (byte) v;
      overflow = v >>> 8;
    }
    this.data = result;
  }

  // old add-method with BigInteger quick hack
  public void add2(DataWord word) {
    BigInteger result = value().add(word.value());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  // mul can be done in more efficient way
  // with shift left shift right trick
  // without BigInteger quick hack
  public void mul(DataWord word) {
    BigInteger result = value().multiply(word.value());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  // improve with no BigInteger
  public void div(DataWord word) {

    if (word.isZero()) {
      this.and(ZERO);
      return;
    }

    BigInteger result = value().divide(word.value());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  // improve with no BigInteger
  public void sDiv(DataWord word) {

    if (word.isZero()) {
      this.and(ZERO);
      return;
    }

    BigInteger result = sValue().divide(word.sValue());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  // improve with no BigInteger
  public void sub(DataWord word) {
    BigInteger result = value().subtract(word.value());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  // improve with no BigInteger
  public void exp(DataWord word) {
    BigInteger result = value().modPow(word.value(), _2_256);
    this.data = ByteUtil.copyToArray(result);
  }

  // improve with no BigInteger
  public void mod(DataWord word) {

    if (word.isZero()) {
      this.and(ZERO);
      return;
    }

    BigInteger result = value().mod(word.value());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  public void sMod(DataWord word) {

    if (word.isZero()) {
      this.and(ZERO());
      return;
    }

    BigInteger result = sValue().abs().mod(word.sValue().abs());
    result = (sValue().signum() == -1) ? result.negate() : result;

    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  public void addmod(DataWord word1, DataWord word2) {
    if (word2.isZero()) {
      this.data = new byte[32];
      return;
    }

    BigInteger result = value().add(word1.value()).mod(word2.value());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  public void mulmod(DataWord word1, DataWord word2) {

    if (this.isZero() || word1.isZero() || word2.isZero()) {
      this.data = new byte[32];
      return;
    }

    BigInteger result = value().multiply(word1.value()).mod(word2.value());
    this.data = ByteUtil.copyToArray(result.and(MAX_VALUE));
  }

  @JsonValue
  @Override
  public String toString() {
    return Hex.toHexString(data);
  }

  public String toPrefixString() {

    byte[] pref = getNoLeadZeroesData();
    if (pref.length == 0) {
      return "";
    }

    if (pref.length < 7) {
      return Hex.toHexString(pref);
    }

    return Hex.toHexString(pref).substring(0, 6);
  }

  public String shortHex() {
    String hexValue = Hex.toHexString(getNoLeadZeroesData()).toUpperCase();
    return "0x" + hexValue.replaceFirst("^0+(?!$)", "");
  }

  public DataWord clone() {
    return new DataWord(Arrays.clone(data));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DataWord dataWord = (DataWord) o;

    return java.util.Arrays.equals(data, dataWord.data);

  }

  @Override
  public int hashCode() {
    return java.util.Arrays.hashCode(data);
  }

  @Override
  public int compareTo(DataWord o) {
    if (o == null || o.getData() == null) {
      return -1;
    }
    int result = FastByteComparisons.compareTo(
        data, 0, data.length,
        o.getData(), 0, o.getData().length);
    // Convert result into -1, 0 or 1 as is the convention
    return (int) Math.signum(result);
  }

  public void signExtend(byte k) {
    if (0 > k || k > 31) {
      throw new IndexOutOfBoundsException();
    }
    byte mask = this.sValue().testBit((k * 8) + 7) ? (byte) 0xff : 0;
    for (int i = 31; i > k; i--) {
      this.data[31 - i] = mask;
    }
  }

  public int bytesOccupied() {
    int firstNonZero = ByteUtil.firstNonZeroByte(data);
    if (firstNonZero == -1) {
      return 0;
    }
    return 31 - firstNonZero + 1;
  }

  public boolean isHex(String hex) {
    return Hex.toHexString(data).equals(hex);
  }

  public String asString() {
    return new String(getNoLeadZeroesData());
  }

  public String toHexString() {
    return Hex.toHexString(data);
  }

  /**
   * Shift left, both this and input arg are treated as unsigned
   *
   * @return this << arg
   */
  public DataWord shiftLeft(DataWord arg) {
    if (arg.value().compareTo(BigInteger.valueOf(MAX_POW)) >= 0) {
      return DataWord.ZERO();
    }

    BigInteger result = value().shiftLeft(arg.intValueSafe());
    return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
  }

  /**
   * Shift right, both this and input arg are treated as unsigned
   *
   * @return this >> arg
   */
  public DataWord shiftRight(DataWord arg) {
    if (arg.value().compareTo(BigInteger.valueOf(MAX_POW)) >= 0) {
      return DataWord.ZERO();
    }

    BigInteger result = value().shiftRight(arg.intValueSafe());
    return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
  }

  /**
   * Shift right, this is signed, while input arg is treated as unsigned
   *
   * @return this >> arg
   */
  public DataWord shiftRightSigned(DataWord arg) {
    if (arg.value().compareTo(BigInteger.valueOf(MAX_POW)) >= 0) {
      if (this.isNegative()) {
        DataWord result = ONE();
        result.negate();
        return result;
      } else {
        return ZERO();
      }
    }

    BigInteger result = sValue().shiftRight(arg.intValueSafe());
    return new DataWord(ByteUtil.copyToArray(result.and(MAX_VALUE)));
  }
}

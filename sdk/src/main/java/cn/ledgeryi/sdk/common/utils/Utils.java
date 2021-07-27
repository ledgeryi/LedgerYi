/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package cn.ledgeryi.sdk.common.utils;

import cn.ledgeryi.sdk.keystore.StringUtils;
import com.google.protobuf.Message;

import java.io.Console;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Utils {
  public static final String PERMISSION_ID = "Permission_id";
  public static final String VISIBLE = "visible";
  public static final String TRANSACTION = "transaction";
  public static final String VALUE = "value";

  private static SecureRandom random = new SecureRandom();

  public static SecureRandom getRandom() {
    return random;
  }

  public static byte[] getBytes(char[] chars) {
    Charset cs = Charset.forName("UTF-8");
    CharBuffer cb = CharBuffer.allocate(chars.length);
    cb.put(chars);
    cb.flip();
    ByteBuffer bb = cs.encode(cb);

    return bb.array();
  }

  private char[] getChars(byte[] bytes) {
    Charset cs = Charset.forName("UTF-8");
    ByteBuffer bb = ByteBuffer.allocate(bytes.length);
    bb.put(bytes);
    bb.flip();
    CharBuffer cb = cs.decode(bb);

    return cb.array();
  }

  /** yyyy-MM-dd */
  public static Date strToDateLong(String strDate) {
    if (strDate.length() == 10) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      ParsePosition pos = new ParsePosition(0);
      Date strtodate = formatter.parse(strDate, pos);
      return strtodate;
    } else if (strDate.length() == 19) {
      strDate = strDate.replace("_", " ");
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      ParsePosition pos = new ParsePosition(0);
      Date strtodate = formatter.parse(strDate, pos);
      return strtodate;
    }
    return null;
  }

  public static String formatMessageString(Message message) {
    String result = JsonFormat.printToString(message, true);
    return JsonFormatUtil.formatJson(result);
  }

  public static char[] inputPassword2Twice() throws IOException {
    char[] password0;
    while (true) {
      System.out.println("Please input password.");
      password0 = Utils.inputPassword(true);
      System.out.println("Please input password again.");
      char[] password1 = Utils.inputPassword(true);
      boolean flag = Arrays.equals(password0, password1);
      StringUtils.clear(password1);
      if (flag) {
        break;
      }
      System.out.println("The passwords do not match, please input again.");
    }
    return password0;
  }

  public static char[] inputPassword(boolean checkStrength) throws IOException {
    char[] password;
    Console cons = System.console();
    while (true) {
      if (cons != null) {
        password = cons.readPassword("password: ");
      } else {
        byte[] passwd0 = new byte[64];
        int len = System.in.read(passwd0, 0, passwd0.length);
        int i;
        for (i = 0; i < len; i++) {
          if (passwd0[i] == 0x09 || passwd0[i] == 0x0A) {
            break;
          }
        }
        byte[] passwd1 = Arrays.copyOfRange(passwd0, 0, i);
        password = StringUtils.byte2Char(passwd1);
        StringUtils.clear(passwd0);
        StringUtils.clear(passwd1);
      }
      if (LedgerYiApi.passwordValid(password)) {
        return password;
      }
      if (!checkStrength) {
        return password;
      }
      StringUtils.clear(password);
      System.out.println("Invalid password, please input again.");
    }
  }



}

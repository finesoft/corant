/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.wxpay.sdk;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * corant-modules-cloud-tencent
 *
 * @author bingo 上午10:05:42
 */
@XmlRootElement(name = "xml")
public class CorantWXNotifyXMLVO {
  private String appid;
  private String mch_id;
  private String nonce_str;
  private String sign;
  private String return_code;
  private String fee_type;
  private String sign_type;
  private String result_code;
  private String openid;
  private String is_subscribe;
  private String trade_type;
  private String bank_type;
  private String total_fee;
  private String cash_fee;
  private String transaction_id;
  private String out_trade_no;
  private String time_end;

  public String getAppid() {
    return appid;
  }

  public String getBank_type() {
    return bank_type;
  }

  public String getCash_fee() {
    return cash_fee;
  }

  public String getFee_type() {
    return fee_type;
  }

  public String getIs_subscribe() {
    return is_subscribe;
  }

  public String getMch_id() {
    return mch_id;
  }

  public String getNonce_str() {
    return nonce_str;
  }

  public String getOpenid() {
    return openid;
  }

  public String getOut_trade_no() {
    return out_trade_no;
  }

  public String getResult_code() {
    return result_code;
  }

  public String getReturn_code() {
    return return_code;
  }

  public String getSign() {
    return sign;
  }

  public String getSign_type() {
    return sign_type;
  }

  public String getTime_end() {
    return time_end;
  }

  public String getTotal_fee() {
    return total_fee;
  }

  public String getTrade_type() {
    return trade_type;
  }

  public String getTransaction_id() {
    return transaction_id;
  }

  public CorantWXNotifyXMLVO setAppid(String appid) {
    this.appid = appid;
    return this;
  }

  public CorantWXNotifyXMLVO setBank_type(String bank_type) {
    this.bank_type = bank_type;
    return this;
  }

  public CorantWXNotifyXMLVO setCash_fee(String cash_fee) {
    this.cash_fee = cash_fee;
    return this;
  }

  public CorantWXNotifyXMLVO setFee_type(String fee_type) {
    this.fee_type = fee_type;
    return this;
  }

  public CorantWXNotifyXMLVO setIs_subscribe(String is_subscribe) {
    this.is_subscribe = is_subscribe;
    return this;
  }

  public CorantWXNotifyXMLVO setMch_id(String mch_id) {
    this.mch_id = mch_id;
    return this;
  }

  public CorantWXNotifyXMLVO setNonce_str(String nonce_str) {
    this.nonce_str = nonce_str;
    return this;
  }

  public CorantWXNotifyXMLVO setOpenid(String openid) {
    this.openid = openid;
    return this;
  }

  public CorantWXNotifyXMLVO setOut_trade_no(String out_trade_no) {
    this.out_trade_no = out_trade_no;
    return this;
  }

  public CorantWXNotifyXMLVO setResult_code(String result_code) {
    this.result_code = result_code;
    return this;
  }

  public CorantWXNotifyXMLVO setReturn_code(String return_code) {
    this.return_code = return_code;
    return this;
  }

  public CorantWXNotifyXMLVO setSign(String sign) {
    this.sign = sign;
    return this;
  }

  public CorantWXNotifyXMLVO setSign_type(String sign_type) {
    this.sign_type = sign_type;
    return this;
  }

  public CorantWXNotifyXMLVO setTime_end(String time_end) {
    this.time_end = time_end;
    return this;
  }

  public CorantWXNotifyXMLVO setTotal_fee(String total_fee) {
    this.total_fee = total_fee;
    return this;
  }

  public CorantWXNotifyXMLVO setTrade_type(String trade_type) {
    this.trade_type = trade_type;
    return this;
  }

  public CorantWXNotifyXMLVO setTransaction_id(String transaction_id) {
    this.transaction_id = transaction_id;
    return this;
  }
}

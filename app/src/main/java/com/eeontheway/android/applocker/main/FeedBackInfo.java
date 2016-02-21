package com.eeontheway.android.applocker.main;

/**
 * 用户反馈信息
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class FeedBackInfo {
	/**
	 * 反馈的内容
	 */
	private String content;

	/**
	 * 联系方式
	 */
	private String contact;

	/**
	 * 创建时间
	 */
	private String time;

	/**
	 * 获取反馈内容
	 * @return 反馈内容
     */
	public String getContent() {
		return content;
	}

	/**
	 * 设置反馈内容
	 * @param content 反馈内容
     */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * 获取联系方式
	 * @return 联系方式
     */
	public String getContact() {
		return contact;
	}

	/**
	 * 设置联系方式
	 * @param contact 联系方式
     */
	public void setContact(String contact) {
		this.contact = contact;
	}

	/**
	 * 获取创建时间
	 * @return 创建时间
     */
	public String getCreateTime() {
		return time;
	}

	/**
	 * 设置创建时间
	 * @param time 创建时间
     */
	public void setCreateTime(String time) {
		this.time = time;
	}
}

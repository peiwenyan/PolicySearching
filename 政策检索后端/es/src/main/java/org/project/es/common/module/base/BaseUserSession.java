package org.project.es.common.module.base;

import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.IBean;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings("serial")
public abstract class BaseUserSession<M extends BaseUserSession<M>> extends Model<M> implements IBean {

	/**
	 * 主键 自增主键
	 */
	public void setId(java.math.BigInteger id) {
		set("id", id);
	}
	
	/**
	 * 主键 自增主键
	 */
	public java.math.BigInteger getId() {
		return get("id");
	}
	
	/**
	 * 登录方式 1-WEB 2-小程序
	 */
	public void setLoginType(java.lang.Integer loginType) {
		set("login_type", loginType);
	}
	
	/**
	 * 登录方式 1-WEB 2-小程序
	 */
	public java.lang.Integer getLoginType() {
		return getInt("login_type");
	}
	
	/**
	 * 用户ID 对应user表id
	 */
	public void setUserId(java.math.BigInteger userId) {
		set("user_id", userId);
	}
	
	/**
	 * 用户ID 对应user表id
	 */
	public java.math.BigInteger getUserId() {
		return get("user_id");
	}
	
	/**
	 * 统一认证信息
	 */
	public void setAccessToken(java.lang.String accessToken) {
		set("access_token", accessToken);
	}
	
	/**
	 * 统一认证信息
	 */
	public java.lang.String getAccessToken() {
		return getStr("access_token");
	}
	
	/**
	 * session有效时间 单位s，0表示永久有效
	 */
	public void setExpiresIn(java.lang.Long expiresIn) {
		set("expires_in", expiresIn);
	}
	
	/**
	 * session有效时间 单位s，0表示永久有效
	 */
	public java.lang.Long getExpiresIn() {
		return getLong("expires_in");
	}
	
	/**
	 * session过期时间 0表示永久有效，否则为session过期时间
	 */
	public void setExpiresTime(java.lang.Long expiresTime) {
		set("expires_time", expiresTime);
	}
	
	/**
	 * session过期时间 0表示永久有效，否则为session过期时间
	 */
	public java.lang.Long getExpiresTime() {
		return getLong("expires_time");
	}
	
	/**
	 * 是否被强制退出 0-否 1-是
	 */
	public void setIsForceQuit(java.lang.Integer isForceQuit) {
		set("is_force_quit", isForceQuit);
	}
	
	/**
	 * 是否被强制退出 0-否 1-是
	 */
	public java.lang.Integer getIsForceQuit() {
		return getInt("is_force_quit");
	}
	
	/**
	 * 是否删除 0-未删除 1-已删除
	 */
	public void setIsDeleted(java.lang.Integer isDeleted) {
		set("is_deleted", isDeleted);
	}
	
	/**
	 * 是否删除 0-未删除 1-已删除
	 */
	public java.lang.Integer getIsDeleted() {
		return getInt("is_deleted");
	}
	
	/**
	 * 创建时间 创建时间
	 */
	public void setCreatedTime(java.util.Date createdTime) {
		set("created_time", createdTime);
	}
	
	/**
	 * 创建时间 创建时间
	 */
	public java.util.Date getCreatedTime() {
		return getDate("created_time");
	}
	
	/**
	 * 更新时间 更新时间
	 */
	public void setUpdatedTime(java.util.Date updatedTime) {
		set("updated_time", updatedTime);
	}
	
	/**
	 * 更新时间 更新时间
	 */
	public java.util.Date getUpdatedTime() {
		return getDate("updated_time");
	}
	
}


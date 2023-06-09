package org.project.es.common.module.base;

import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.IBean;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings("serial")
public abstract class BaseUserSearchHistory<M extends BaseUserSearchHistory<M>> extends Model<M> implements IBean {

	/**
	 * 自增主键 自增主键
	 */
	public void setId(java.math.BigInteger id) {
		set("id", id);
	}
	
	/**
	 * 自增主键 自增主键
	 */
	public java.math.BigInteger getId() {
		return get("id");
	}
	
	/**
	 * 用户标识号
	 */
	public void setUserId(java.math.BigInteger userId) {
		set("user_id", userId);
	}
	
	/**
	 * 用户标识号
	 */
	public java.math.BigInteger getUserId() {
		return get("user_id");
	}
	
	/**
	 * 搜索内容
	 */
	public void setSearchContent(java.lang.String searchContent) {
		set("search_content", searchContent);
	}
	
	/**
	 * 搜索内容
	 */
	public java.lang.String getSearchContent() {
		return getStr("search_content");
	}
	
	/**
	 * 是否删除 0-未删除；1-已删除
	 */
	public void setIsDeleted(java.lang.Integer isDeleted) {
		set("is_deleted", isDeleted);
	}
	
	/**
	 * 是否删除 0-未删除；1-已删除
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


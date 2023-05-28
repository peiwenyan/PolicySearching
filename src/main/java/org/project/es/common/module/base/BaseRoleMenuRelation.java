package org.project.es.common.module.base;

import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.IBean;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings("serial")
public abstract class BaseRoleMenuRelation<M extends BaseRoleMenuRelation<M>> extends Model<M> implements IBean {

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
	 * 角色ID 对应角色表ID
	 */
	public void setRoleId(java.math.BigInteger roleId) {
		set("role_id", roleId);
	}
	
	/**
	 * 角色ID 对应角色表ID
	 */
	public java.math.BigInteger getRoleId() {
		return get("role_id");
	}
	
	/**
	 * 权限菜单ID 对应菜单ID
	 */
	public void setMenuId(java.math.BigInteger menuId) {
		set("menu_id", menuId);
	}
	
	/**
	 * 权限菜单ID 对应菜单ID
	 */
	public java.math.BigInteger getMenuId() {
		return get("menu_id");
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


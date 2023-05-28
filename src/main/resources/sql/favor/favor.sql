#sql("getUserFavorType")
select * from user_favorites_type where user_id=#para(user_id) and is_deleted=0
#end

#sql("getTypeUserFavor")
select * from user_favorites where user_favorites_type_id=#para(user_favorites_type_id) and is_deleted=0
#end

#sql("getUserHistory")
select * from user_browse_history where user_id=#para(user_id) and is_deleted=0
#end
#sql("getByAccount")
select * from user where account=#para(account) and is_deleted=0
#end

#sql("getById")
select * from user where id=#para(id) and is_deleted=0
#end

#sql("updatePassword")
update user
set password=#para(password),salt=#para(salt)
where id=#para(id) and is_deleted=0
#end

#sql("deleteUser")
update user
set is_deleted=1
where id=#para(id)
#end
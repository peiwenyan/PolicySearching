#sql("getSessionById")
select id,session_id,expires_in,expires_time
from user_session
where user_id = #para(id) and is_deleted = 0
order by id desc
#end

#sql("getByToken")
select id,user_id,session_id,expires_in,expires_time
from user_session
where session_id = #para(sessionId) and is_deleted = 0
    #end

#sql("deleteByAccount")
update user_session set is_deleted = 1
where user_id = #para(accountId)
    #end

#sql("deleteByToken")
update user_session set is_deleted = 1
where session_id = #para(token)
    #end

#sql("list")
select us.id,us.session_id,u.name,u.account,li.ip_address,li.login_location,li.browser,li.os,li.created_time
from user_session us
         join login_info li on li.session_id = us.id
         join user u on u.id = us.user_id
    #if(rq.userId != 0)
         and u.id = #para(rq.userId)
        #end
where (us.expires_time = 0 or us.expires_time > #para(rq.now)) and us.is_deleted = 0
    #end
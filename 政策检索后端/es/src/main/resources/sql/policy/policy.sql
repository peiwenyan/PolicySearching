#sql("list")
select * from policy where is_deleted=0
#end

#sql("get")
select * from policy where id=#para(id) and is_deleted=0
#end

#sql("getPolicyKey")
select * from policy_key where id=#para(id) and is_deleted=0
#end

#sql("getUserSearch")
select * from user_search_history
    where user_id=#para(id)
    and search_content like '%#(value)%'
    and is_deleted=0
#end

#sql("getUserAllSearch")
select id,search_content from user_search_history
where user_id=#para(id)
  and is_deleted=0
#end

#sql("getAllSearch")
select search_content from user_search_history
where is_deleted=0
    #end


#sql("getUserExistSearch")
select * from user_search_history
where user_id=#para(id)
  and search_content=#para(search)
  and is_deleted=0
    #end


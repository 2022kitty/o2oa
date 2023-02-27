package com.x.attendance.assemble.control.jaxrs.v2.detail;

import com.google.gson.JsonElement;
import com.x.attendance.assemble.control.Business;
import com.x.attendance.assemble.control.jaxrs.v2.ExceptionEmptyParameter;
import com.x.attendance.assemble.control.jaxrs.v2.workplace.ActionListWithWorkPlaceObject;
import com.x.attendance.entity.v2.*;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fancyLou on 2023/2/27.
 * Copyright © 2023 O2. All rights reserved.
 */
public class ActionListByPage extends BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionListByPage.class);


    ActionResult<List<Wo>> execute(Integer page, Integer size, JsonElement jsonElement) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" ActionListByPage page:{}, size:{}.", page, size);
        }
        try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
            Wi wi = this.convertToWrapIn(jsonElement, Wi.class);
            if (wi == null || StringUtils.isEmpty(wi.getStartDate()) || StringUtils.isEmpty(wi.getEndDate())) {
                throw new ExceptionEmptyParameter("日期");
            }
            ActionResult<List<Wo>> result = new ActionResult<>();
            Business business = new Business(emc);
            Integer adjustPage = this.adjustPage(page);
            Integer adjustPageSize = this.adjustSize(size);
            List<AttendanceV2Detail> list = business.getAttendanceV2ManagerFactory().listDetailByPage(adjustPage,
                    adjustPageSize, wi.getUserId(), wi.getStartDate(), wi.getEndDate());
            List<Wo> wos =  Wo.copier.copy(list);
            if (wos != null && !wos.isEmpty()) {
                for (Wo detail : wos) {
                    List<String> ids = detail.getRecordIdList();
                    if (ids != null && !ids.isEmpty()) {
                        List<AttendanceV2CheckInRecord> recordList = new ArrayList<>();
                        for (String id : ids) {
                            AttendanceV2CheckInRecord record = emc.find(id, AttendanceV2CheckInRecord.class);
                            if (record != null) {
                                recordList.add(record);
                            }
                        }
                        detail.setRecordList(recordList);
                    }
                }
            }
            result.setData(wos);
            result.setCount(business.getAttendanceV2ManagerFactory().detailCount(wi.getUserId(), wi.getStartDate(), wi.getEndDate()));
            return result;
        }

    }


    public static class Wi extends GsonPropertyObject {


        private static final long serialVersionUID = 5957606815096636450L;


        @FieldDescribe("用户标识")
        private String userId;

        @FieldDescribe("开始日期")
        private String startDate;
        @FieldDescribe("结束日期")
        private String endDate;


        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }

    public static class Wo extends AttendanceV2Detail {
        static WrapCopier<AttendanceV2Detail, Wo> copier = WrapCopierFactory.wo(AttendanceV2Detail.class, Wo.class, null,
                JpaObject.FieldsInvisible);

        @FieldDescribe("打卡记录")
        private List<AttendanceV2CheckInRecord> recordList;

        public List<AttendanceV2CheckInRecord> getRecordList() {
            return recordList;
        }

        public void setRecordList(List<AttendanceV2CheckInRecord> recordList) {
            this.recordList = recordList;
        }
    }
}

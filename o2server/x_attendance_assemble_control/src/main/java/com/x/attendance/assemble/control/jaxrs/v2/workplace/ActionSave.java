package com.x.attendance.assemble.control.jaxrs.v2.workplace;

import com.google.gson.JsonElement;
import com.x.attendance.assemble.control.jaxrs.v2.ExceptionCannotRepetitive;
import com.x.attendance.assemble.control.jaxrs.workplace.BaseAction;
import com.x.attendance.assemble.control.jaxrs.workplace.ExceptionLatitudeEmpty;
import com.x.attendance.assemble.control.jaxrs.workplace.ExceptionLongitudeEmpty;
import com.x.attendance.assemble.control.jaxrs.workplace.ExceptionWorkPlaceNameEmpty;
import com.x.attendance.entity.v2.AttendanceV2Shift;
import com.x.attendance.entity.v2.AttendanceV2WorkPlace;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.entity.annotation.CheckPersistType;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ActionSave extends BaseAction {

	private static  Logger logger = LoggerFactory.getLogger(ActionSave.class);

	protected ActionResult<Wo> execute( EffectivePerson effectivePerson,
			JsonElement jsonElement) throws Exception {
		ActionResult<Wo> result = new ActionResult<>();
		Wi wrapIn = this.convertToWrapIn(jsonElement, Wi.class);
		if (StringUtils.isBlank(wrapIn.getPlaceName())) {
			throw new ExceptionWorkPlaceNameEmpty();
		}

		if (StringUtils.isBlank(wrapIn.getLatitude())) {
			throw new ExceptionLatitudeEmpty();
		}
		if (StringUtils.isBlank(wrapIn.getLongitude())) {
			throw new ExceptionLongitudeEmpty();
		}
		if (StringUtils.isBlank(wrapIn.getPlaceAlias())) {
			wrapIn.setPlaceAlias(wrapIn.getPlaceName());
		}
		if (wrapIn.getErrorRange() == null || wrapIn.getErrorRange() == 0) {
			wrapIn.setErrorRange(200);
		}
		wrapIn.setCreator(effectivePerson.getDistinguishedName());
		if (logger.isDebugEnabled()) {
			logger.debug("保存工作地点，{}", wrapIn.toString());
		}
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			// 名称不能相同
			List<AttendanceV2WorkPlace> checkRepetitive = emc.listEqual(AttendanceV2WorkPlace.class, AttendanceV2WorkPlace.placeName_FIELDNAME, wrapIn.getPlaceName());
			if (checkRepetitive != null && !checkRepetitive.isEmpty()) {
				for (AttendanceV2WorkPlace check : checkRepetitive) {
					if (check.getPlaceName().equals(wrapIn.getPlaceName()) && !check.getId().equals(wrapIn.getId())) {
						throw new ExceptionCannotRepetitive("场所名称");
					}
				}
			}

			AttendanceV2WorkPlace workPlace = new AttendanceV2WorkPlace();
			Wi.copier.copy(wrapIn, workPlace);
			emc.beginTransaction(AttendanceV2WorkPlace.class);
			if (wrapIn.getId() != null && !wrapIn.getId().isEmpty()) {
				AttendanceV2WorkPlace old = emc.find(wrapIn.getId(), AttendanceV2WorkPlace.class);
				if (old != null) {
					workPlace.copyTo(old, JpaObject.FieldsUnmodify);
					emc.check(old, CheckPersistType.all);
					Wo wo = new Wo(old.getId());
					result.setData(wo);
				} else {
					emc.persist(workPlace, CheckPersistType.all);
					Wo wo = new Wo(workPlace.getId());
					result.setData(wo);
				}
			} else {
				emc.persist(workPlace, CheckPersistType.all);
				Wo wo = new Wo(workPlace.getId());
				result.setData(wo);
			}
			emc.commit();
		}

		return result;
	}

	public static class Wi extends AttendanceV2WorkPlace {

		private static final long serialVersionUID = 2861460559469362660L;
		public static WrapCopier<Wi, AttendanceV2WorkPlace> copier = WrapCopierFactory.wi(Wi.class,
				AttendanceV2WorkPlace.class, null, JpaObject.FieldsUnmodify);

	}

	public static class Wo extends WoId {
		public Wo(String id) {
			setId(id);
		}
	}
}
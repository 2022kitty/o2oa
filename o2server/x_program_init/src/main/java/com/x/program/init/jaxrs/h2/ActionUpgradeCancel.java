package com.x.program.init.jaxrs.h2;

import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WrapBoolean;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.program.init.ThisApplication;

class ActionUpgradeCancel extends BaseAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionUpgradeCancel.class);

	public ActionResult<Wo> execute(EffectivePerson effectivePerson) {
		LOGGER.debug("execute:{}.", effectivePerson::getDistinguishedName);
		ActionResult<Wo> result = new ActionResult<>();
		ThisApplication.setMissionUpgradeH2(null);
		Wo wo = new Wo();
		wo.setValue(true);
		result.setData(wo);
		return result;
	}

	public static class Wo extends WrapBoolean {

		private static final long serialVersionUID = 7688723596987345774L;

	}

}
/**
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/
*
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
*
* The Original Code is OpenELIS code.
*
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/
package us.mn.state.health.lims.systemusersection.daoimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import us.mn.state.health.lims.audittrail.dao.AuditTrailDAO;
import us.mn.state.health.lims.audittrail.daoimpl.AuditTrailDAOImpl;
import us.mn.state.health.lims.common.action.IActionConstants;
import us.mn.state.health.lims.common.daoimpl.BaseDAOImpl;
import us.mn.state.health.lims.common.exception.LIMSDuplicateRecordException;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.log.LogEvent;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.systemusersection.dao.SystemUserSectionDAO;
import us.mn.state.health.lims.systemusersection.valueholder.SystemUserSection;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
@Component
@Transactional 
public class SystemUserSectionDAOImpl extends BaseDAOImpl<SystemUserSection, String> implements SystemUserSectionDAO {

	public SystemUserSectionDAOImpl() {
		super(SystemUserSection.class);
	}

	@Override
	public void deleteData(List systemUserSections) throws LIMSRuntimeException {
		// add to audit trail
		try {
			AuditTrailDAO auditDAO = new AuditTrailDAOImpl();
			for (int i = 0; i < systemUserSections.size(); i++) {
				SystemUserSection data = (SystemUserSection) systemUserSections.get(i);

				SystemUserSection oldData = readSystemUserSection(data.getId());
				SystemUserSection newData = new SystemUserSection();

				String sysUserId = data.getSysUserId();
				String event = IActionConstants.AUDIT_TRAIL_DELETE;
				String tableName = "SYSTEM_USER_SECTION";
				auditDAO.saveHistory(newData, oldData, sysUserId, event, tableName);
			}
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "AuditTrail deleteData()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection AuditTrail deleteData()", e);
		}

		try {
			for (int i = 0; i < systemUserSections.size(); i++) {
				SystemUserSection data = (SystemUserSection) systemUserSections.get(i);
				// bugzilla 2206
				data = readSystemUserSection(data.getId());
				sessionFactory.getCurrentSession().delete(data);
				// sessionFactory.getCurrentSession().flush(); // CSL remove old
				// sessionFactory.getCurrentSession().clear(); // CSL remove old
			}
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "deleteData()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection deleteData()", e);
		}
	}

	@Override
	public boolean insertData(SystemUserSection systemUserSection) throws LIMSRuntimeException {

		try {
			if (duplicateSystemUserSectionExists(systemUserSection)) {
				throw new LIMSDuplicateRecordException(
						"Duplicate record exists for " + systemUserSection.getSysUserId());
			}

			String id = (String) sessionFactory.getCurrentSession().save(systemUserSection);
			systemUserSection.setId(id);

			// add to audit trail
			AuditTrailDAO auditDAO = new AuditTrailDAOImpl();
			String sysUserId = systemUserSection.getSysUserId();
			String tableName = "SYSTEM_USER_SECTION";
			auditDAO.saveNewHistory(systemUserSection, sysUserId, tableName);

			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "insertData()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection insertData()", e);
		}

		return true;
	}

	@Override
	public void updateData(SystemUserSection systemUserSection) throws LIMSRuntimeException {

		try {
			if (duplicateSystemUserSectionExists(systemUserSection)) {
				throw new LIMSDuplicateRecordException(
						"Duplicate record exists for " + systemUserSection.getSystemUserId());
			}
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "updateData()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection updateData()", e);
		}

		SystemUserSection oldData = readSystemUserSection(systemUserSection.getId());
		SystemUserSection newData = systemUserSection;

		// add to audit trail
		try {
			AuditTrailDAO auditDAO = new AuditTrailDAOImpl();
			String sysUserId = systemUserSection.getSysUserId();
			String event = IActionConstants.AUDIT_TRAIL_UPDATE;
			String tableName = "SYSTEM_USER_SECTION";
			auditDAO.saveHistory(newData, oldData, sysUserId, event, tableName);
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "AuditTrail updateData()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection AuditTrail updateData()", e);
		}

		try {
			sessionFactory.getCurrentSession().merge(systemUserSection);
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
			// sessionFactory.getCurrentSession().evict // CSL remove old(systemUserSection);
			// sessionFactory.getCurrentSession().refresh // CSL remove old(systemUserSection);
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "updateData()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection updateData()", e);
		}
	}

	@Override
	public void getData(SystemUserSection systemUserSection) throws LIMSRuntimeException {
		try {
			SystemUserSection sysUserSection = (SystemUserSection) sessionFactory.getCurrentSession()
					.get(SystemUserSection.class, systemUserSection.getId());
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
			if (sysUserSection != null) {
				PropertyUtils.copyProperties(systemUserSection, sysUserSection);
			} else {
				systemUserSection.setId(null);
			}
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "getData()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection getData()", e);
		}
	}

	@Override
	public List getAllSystemUserSections() throws LIMSRuntimeException {
		List list = new Vector();
		try {
			String sql = "from SystemUserSection";
			org.hibernate.Query query = sessionFactory.getCurrentSession().createQuery(sql);
			list = query.list();
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "getAllSystemModules()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection getAllSystemModules()", e);
		}

		return list;
	}

	@Override
	public List getAllSystemUserSectionsBySystemUserId(int systemUserId) throws LIMSRuntimeException {
		List list = new Vector();
		try {
			String sql = "from SystemUserSection s where s.systemUser.id = :param";
			org.hibernate.Query query = sessionFactory.getCurrentSession().createQuery(sql);
			query.setParameter("param", systemUserId);
			list = query.list();
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "getAllSystemUserSectionsBySystemUserId()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection getAllSystemUserSectionsBySystemUserId()", e);
		}

		return list;
	}

	@Override
	public List getPageOfSystemUserSections(int startingRecNo) throws LIMSRuntimeException {
		List list = new Vector();
		try {
			// calculate maxRow to be one more than the page size
			int endingRecNo = startingRecNo + (SystemConfiguration.getInstance().getDefaultPageSize() + 1);

			String sql = "from SystemUserSection s ";
			org.hibernate.Query query = sessionFactory.getCurrentSession().createQuery(sql);
			query.setFirstResult(startingRecNo - 1);
			query.setMaxResults(endingRecNo - 1);

			list = query.list();
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "getPageOfSystemUserSections()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection getPageOfSystemUserSections()", e);
		}

		return list;
	}

	public SystemUserSection readSystemUserSection(String idString) {
		SystemUserSection sysUserSection = null;
		try {
			sysUserSection = (SystemUserSection) sessionFactory.getCurrentSession().get(SystemUserSection.class, idString);
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "readSystemUserSection()", e.toString());
			throw new LIMSRuntimeException("Error in SystemUserSection readSystemUserSection(idString)", e);
		}

		return sysUserSection;
	}

	@Override
	public List getNextSystemUserSectionRecord(String id) throws LIMSRuntimeException {

		return getNextRecord(id, "SystemUserSection", SystemUserSection.class);
	}

	@Override
	public List getPreviousSystemUserSectionRecord(String id) throws LIMSRuntimeException {

		return getPreviousRecord(id, "SystemUserSection", SystemUserSection.class);
	}

	@Override
	public Integer getTotalSystemUserSectionCount() throws LIMSRuntimeException {
		return getTotalCount("SystemUserSection", SystemUserSection.class);
	}

	@Override
	public List getNextRecord(String id, String table, Class clazz) throws LIMSRuntimeException {
		int currentId = (Integer.valueOf(id)).intValue();
		String tablePrefix = getTablePrefix(table);

		List list = new Vector();
		int rrn = 0;
		try {
			String sql = "select sus.id from SystemUserSection sus order by sus.systemUser.id";
			org.hibernate.Query query = sessionFactory.getCurrentSession().createQuery(sql);
			list = query.list();
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
			rrn = list.indexOf(String.valueOf(currentId));

			list = sessionFactory.getCurrentSession().getNamedQuery(tablePrefix + "getNext").setFirstResult(rrn + 1)
					.setMaxResults(2).list();

		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "getNextRecord()", e.toString());
			throw new LIMSRuntimeException("Error in getNextRecord() for " + table, e);
		}

		return list;
	}

	@Override
	public List getPreviousRecord(String id, String table, Class clazz) throws LIMSRuntimeException {
		int currentId = (Integer.valueOf(id)).intValue();
		String tablePrefix = getTablePrefix(table);

		List list = new Vector();
		int rrn = 0;
		try {
			String sql = "select sus.id from SystemUserSection sus order by sus.systemUser.id";
			org.hibernate.Query query = sessionFactory.getCurrentSession().createQuery(sql);
			list = query.list();
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old
			rrn = list.indexOf(String.valueOf(currentId));

			list = sessionFactory.getCurrentSession().getNamedQuery(tablePrefix + "getPrevious").setFirstResult(rrn + 1)
					.setMaxResults(2).list();

		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "getPreviousRecord()", e.toString());
			throw new LIMSRuntimeException("Error in getPreviousRecord() for " + table, e);
		}

		return list;
	}

	private boolean duplicateSystemUserSectionExists(SystemUserSection systemUserSection) throws LIMSRuntimeException {
		try {

			List list = new ArrayList();

			String sql = "from SystemUserSection s where s.systemUser.id = :param and s.testSection.id = :param2 and s.id != :param3";
			org.hibernate.Query query = sessionFactory.getCurrentSession().createQuery(sql);
			query.setParameter("param", systemUserSection.getSystemUser().getId());
			query.setParameter("param2", systemUserSection.getTestSection().getId());

			String systemUserSectionId = "0";
			if (!StringUtil.isNullorNill(systemUserSection.getId())) {
				systemUserSectionId = systemUserSection.getId();
			}
			query.setParameter("param3", systemUserSectionId);

			list = query.list();
			// sessionFactory.getCurrentSession().flush(); // CSL remove old
			// sessionFactory.getCurrentSession().clear(); // CSL remove old

			if (list.size() > 0) {
				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
			// bugzilla 2154
			LogEvent.logError("SystemUserSectionDAOImpl", "duplicateSystemUserSectionExists()", e.toString());
			throw new LIMSRuntimeException("Error in duplicateSystemUserSectionExists()", e);
		}
	}

}
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.asset.entry.set.util;

import com.liferay.compat.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserConstants;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetTag;
import com.liferay.portlet.asset.service.AssetTagLocalServiceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matthew Kong
 */
public class AssetEntrySetParticipantInfoImpl
	implements AssetEntrySetParticipantInfo {

	public ObjectValuePair<Long, Long> getClassNameIdAndClassPKOVP(long userId)
		throws SystemException {

		return new ObjectValuePair<Long, Long>(_USER_CLASS_NAME_ID, userId);
	}

	public JSONObject getParticipantJSONObject(
			JSONObject participantJSONObject, long classNameId, long classPK,
			boolean includeProfileImageURL)
		throws PortalException, SystemException {

		if (classNameId != _USER_CLASS_NAME_ID) {
			return participantJSONObject;
		}

		User user = UserLocalServiceUtil.getUser(classPK);

		participantJSONObject.put(
			AssetEntrySetConstants.ASSET_ENTRY_KEY_PARTICIPANT_FULL_NAME,
			user.getFullName());
		participantJSONObject.put(
			AssetEntrySetConstants.
				ASSET_ENTRY_KEY_PARTICIPANT_PROFILE_IMAGE_URL,
			UserConstants.getPortraitURL(
				PortalUtil.getPathImage(), user.isMale(),
				user.getPortraitId()));

		Group group = user.getGroup();

		participantJSONObject.put(
			AssetEntrySetConstants.ASSET_ENTRY_KEY_PARTICIPANT_URL,
			_LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING +
				group.getFriendlyURL());

		participantJSONObject.put("classNameId", classNameId);
		participantJSONObject.put("classPK", classPK);

		return participantJSONObject;
	}

	public boolean isMember(
		long classNameId, long classPK, long sharedToClassNameId,
		long sharedToClassPK) {

		if (classNameId != _USER_CLASS_NAME_ID) {
			return false;
		}

		if (sharedToClassNameId == _USER_CLASS_NAME_ID) {
			if (classPK == sharedToClassPK) {
				return true;
			}

			return false;
		}

		try {
			if (classNameId == _GROUP_CLASS_NAME_ID) {
				return GroupLocalServiceUtil.hasUserGroup(
					classPK, sharedToClassPK);
			}
		}
		catch (Exception e) {
		}

		return false;
	}

	public String processAssetTagNames(
			long companyId, long userId, String payload)
		throws PortalException, SystemException {

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			payload);

		String[] assetTagNames = StringUtil.split(
			payloadJSONObject.getString(
				AssetEntrySetConstants.PAYLOAD_KEY_ASSET_TAG_NAMES));

		Group group = GroupLocalServiceUtil.getCompanyGroup(companyId);

		AssetTagLocalServiceUtil.checkTags(userId, group, assetTagNames);

		JSONArray payloadSharedToJSONArray =
			payloadJSONObject.getJSONArray(
				AssetEntrySetConstants.PAYLOAD_KEY_SHARED_TO);

		List<Long> assetTagIds = new ArrayList<Long>();

		for (int i = 0; i < payloadSharedToJSONArray.length(); i++) {
			JSONObject sharedToJSONObject =
				payloadSharedToJSONArray.getJSONObject(i);

			if (sharedToJSONObject.getLong("classNameId") !=
					_ASSET_TAG_CLASS_NAME_ID) {

				continue;
			}

			assetTagIds.add(sharedToJSONObject.getLong("classPK"));
		}

		for (String assetTagName : assetTagNames) {
			AssetTag assetTag = AssetTagLocalServiceUtil.getTag(
				group.getGroupId(), assetTagName);

			if (assetTag == null) {
				throw new SystemException(
					"Loop topic does not exist for name " + assetTagName);
			}

			if (assetTagIds.contains(assetTag.getTagId())) {
				continue;
			}

			JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

			jsonObject.put("classNameId", _ASSET_TAG_CLASS_NAME_ID);
			jsonObject.put("classPK", assetTag.getTagId());
			jsonObject.put("name", assetTagName);

			payloadSharedToJSONArray.put(jsonObject);
		}

		return payload;
	}

	private static final long _ASSET_TAG_CLASS_NAME_ID =
		ClassNameLocalServiceUtil.getClassNameId(AssetTag.class);

	private static final long _GROUP_CLASS_NAME_ID =
		ClassNameLocalServiceUtil.getClassNameId(Group.class);

	private static final String _LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING =
		PropsUtil.get(PropsKeys.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING);

	private static final long _USER_CLASS_NAME_ID =
		ClassNameLocalServiceUtil.getClassNameId(User.class);

}
package com.inneo.aisafecodesync.web.service;

import com.inneo.aisafecodesync.core.config.SyncConfig;
import com.inneo.aisafecodesync.core.validation.SyncConfigValidator;
import com.inneo.aisafecodesync.core.validation.ValidationResult;
import com.inneo.aisafecodesync.exception.ConfigValidationException;
import com.inneo.aisafecodesync.persistence.entity.SyncRunEntity;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.persistence.repository.SyncProfileRepository;
import com.inneo.aisafecodesync.persistence.repository.SyncRunRepository;
import com.inneo.aisafecodesync.web.dto.ProfileForm;
import com.inneo.aisafecodesync.web.dto.ProfileYaml;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProfileService {

    private final SyncProfileRepository profileRepository;
    private final SyncRunRepository runRepository;
    private final ProfileMapper profileMapper;
    private final SyncConfigValidator configValidator;
    private final ObjectMapper yamlMapper;

    public ProfileService(SyncProfileRepository profileRepository, SyncRunRepository runRepository, ProfileMapper profileMapper, SyncConfigValidator configValidator) {
        this.profileRepository = profileRepository;
        this.runRepository = runRepository;
        this.profileMapper = profileMapper;
        this.configValidator = configValidator;
        this.yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
    }

    @Transactional(readOnly = true)
    public List<SyncProfileEntity> listProfiles() {
        return profileRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SyncProfileEntity getProfile(long id) {
        return profileRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Profile not found: " + id));
    }

    @Transactional
    public SyncProfileEntity create(ProfileForm form) {
        SyncConfig config = profileMapper.toConfig(form);
        validateForSave(config);
        SyncProfileEntity entity = new SyncProfileEntity();
        profileMapper.updateEntity(entity, form);
        return profileRepository.save(entity);
    }

    @Transactional
    public SyncProfileEntity update(long id, ProfileForm form) {
        SyncConfig config = profileMapper.toConfig(form);
        validateForSave(config);
        SyncProfileEntity entity = getProfile(id);
        profileMapper.updateEntity(entity, form);
        entity.setLastSuccessfulDryRunHash(null);
        entity.setLastSuccessfulDryRunAt(null);
        return profileRepository.save(entity);
    }

    @Transactional
    public void delete(long id) {
        SyncProfileEntity profile = getProfile(id);
        for (SyncRunEntity run : runRepository.findByProfileId(id)) {
            run.setProfile(null);
        }
        profileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public SyncConfig toConfig(long id) {
        return profileMapper.toConfig(getProfile(id));
    }

    @Transactional(readOnly = true)
    public ProfileForm getForm(long id) {
        return profileMapper.toForm(getProfile(id));
    }

    @Transactional(readOnly = true)
    public ValidationResult validate(long id) {
        return configValidator.validate(profileMapper.toConfig(getProfile(id)));
    }

    @Transactional(readOnly = true)
    public String exportYaml(long id) {
        try {
            return yamlMapper.writeValueAsString(profileMapper.toYaml(getProfile(id)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not export profile YAML.", ex);
        }
    }

    @Transactional
    public SyncProfileEntity importYaml(String yaml) {
        try {
            ProfileYaml profileYaml = yamlMapper.readValue(yaml, ProfileYaml.class);
            ProfileForm form = profileMapper.toForm(profileYaml);
            return create(form);
        } catch (JsonProcessingException ex) {
            throw new ConfigValidationException(List.of("Profile YAML could not be parsed: " + ex.getOriginalMessage()));
        }
    }

    public ProfileForm newForm() {
        return profileMapper.newForm();
    }

    private void validateForSave(SyncConfig config) {
        ValidationResult validation = configValidator.validate(config);
        if (!validation.valid()) {
            throw new ConfigValidationException(validation.errors());
        }
    }
}

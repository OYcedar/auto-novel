<script lang="ts" setup>
import { DeleteOutlineOutlined } from '@vicons/material';

import { Locator } from '@/data';
import { GenericNovelId } from '@/model/Common';
import { LocalVolumeMetadata } from '@/model/LocalVolume';
import { Setting } from '@/model/Setting';
import { TranslateTaskDescriptor } from '@/model/Translator';

import LocalVolumeList from './LocalVolumeList.vue';

const props = defineProps<{ type: 'gpt' | 'sakura' }>();

const message = useMessage();

const { setting } = Locator.settingRepository();

const localVolumeListRef = ref<InstanceType<typeof LocalVolumeList>>();

const calculateFinished = (volume: LocalVolumeMetadata) =>
  volume.toc.filter((it) => {
    let chapterGlossaryId: string | undefined;
    if (props.type === 'gpt') {
      chapterGlossaryId = it.gpt;
    } else {
      chapterGlossaryId = it.sakura;
    }
    return chapterGlossaryId === volume.glossaryId;
  }).length;

const calculateExpired = (volume: LocalVolumeMetadata) =>
  volume.toc.filter((it) => {
    let chapterGlossaryId: string | undefined;
    if (props.type === 'gpt') {
      chapterGlossaryId = it.gpt;
    } else {
      chapterGlossaryId = it.sakura;
    }
    return (
      chapterGlossaryId !== undefined && chapterGlossaryId !== volume.glossaryId
    );
  }).length;

const queueAllVolumes = (volumes: LocalVolumeMetadata[]) => {
  volumes.forEach((volume) => queueVolume(volume.id));
};

const queueVolume = (volumeId: string) => {
  const task = TranslateTaskDescriptor.workspace(volumeId, {
    start: 0,
    end: 65535,
    expire: true,
  });

  const workspace =
    props.type === 'gpt'
      ? Locator.gptWorkspaceRepository()
      : Locator.sakuraWorkspaceRepository();

  const success = workspace.addJob({
    task,
    description: volumeId,
    createAt: Date.now(),
  });

  if (success) {
    message.success('排队成功');
  } else {
    message.error('排队失败：翻译任务已经存在');
  }
};

const downloadVolume = async (volumeId: string) => {
  const { mode } = setting.value.downloadFormat;
  const repo = await Locator.localVolumeRepository();

  try {
    const { filename, blob } = await repo.getTranslationFile({
      id: volumeId,
      mode,
      translationsMode: 'priority',
      translations: [props.type],
    });

    const el = document.createElement('a');
    el.href = URL.createObjectURL(blob);
    el.target = '_blank';
    el.download = filename;
    el.click();
  } catch (error) {
    message.error(`文件生成错误：${error}`);
  }
};

const progressFilter = ref<'all' | 'finished' | 'unfinished'>('all');
const progressFilterOptions = [
  { value: 'all', label: '全部' },
  { value: 'finished', label: '已完成' },
  { value: 'unfinished', label: '未完成' },
];
const progressFilterFunc = computed(() => {
  if (progressFilter.value === 'finished') {
    return (volume: LocalVolumeMetadata) => {
      return volume.toc.length === calculateFinished(volume);
    };
  } else if (progressFilter.value === 'unfinished') {
    return (volume: LocalVolumeMetadata) => {
      return volume.toc.length !== calculateFinished(volume);
    };
  } else {
    return undefined;
  }
});
</script>

<template>
  <local-volume-list
    ref="localVolumeListRef"
    :filter="progressFilterFunc"
    :options="{ 全部排队: queueAllVolumes }"
    :beforeVolumeAdd="(file:File)=>queueVolume(file.name)"
  >
    <template #extra>
      <c-action-wrapper title="状态">
        <c-radio-group
          v-model:value="progressFilter"
          :options="progressFilterOptions"
          size="small"
        />
      </c-action-wrapper>

      <c-action-wrapper title="语言">
        <c-radio-group
          v-model:value="setting.downloadFormat.mode"
          :options="Setting.downloadModeOptions"
          size="small"
        />
      </c-action-wrapper>
    </template>

    <template #volume="volume">
      <n-flex :size="4" vertical>
        <n-text>{{ volume.id }}</n-text>

        <n-text depth="3">
          <n-time :time="volume.createAt" type="relative" /> / 总计
          {{ volume.toc.length }} / 完成 {{ calculateFinished(volume) }} / 过期
          {{ calculateExpired(volume) }}
        </n-text>

        <n-flex :size="8">
          <c-button
            label="排队"
            size="tiny"
            secondary
            @action="queueVolume(volume.id)"
          />

          <router-link
            v-if="!volume.id.endsWith('.epub')"
            :to="`/workspace/reader/${encodeURIComponent(volume.id)}/0`"
          >
            <c-button label="阅读" size="tiny" secondary />
          </router-link>

          <c-button
            label="下载"
            size="tiny"
            secondary
            @action="downloadVolume(volume.id)"
          />

          <glossary-button
            :gnid="GenericNovelId.local(volume.id)"
            :value="volume.glossary"
            size="tiny"
            secondary
          />

          <div style="flex: 1" />

          <n-popconfirm
            :show-icon="false"
            @positive-click="localVolumeListRef?.deleteVolume(volume.id)"
            :negative-text="null"
            style="max-width: 300px"
          >
            <template #trigger>
              <c-icon-button :icon="DeleteOutlineOutlined" type="error" />
            </template>
            真的要删除吗？
            <br />
            {{ volume.id }}
          </n-popconfirm>
        </n-flex>
      </n-flex>
    </template>
  </local-volume-list>
</template>

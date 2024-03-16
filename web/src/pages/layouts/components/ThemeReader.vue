<script lang="ts" setup>
import { darkTheme, dateZhCN, zhCN, useOsTheme } from 'naive-ui';
import { computed } from 'vue';

import { useReaderSettingStore } from '@/data/stores/reader_setting';
import { isDarkColor } from '@/pages/reader/components/util';

const readerSetting = useReaderSettingStore();
const osThemeRef = useOsTheme();

const readerTheme = computed(() => {
  let theme: typeof darkTheme | null = null;
  let bodyColor: string | undefined = undefined;

  if (readerSetting.theme.mode === 'light') {
    theme = null;
  } else if (readerSetting.theme.mode === 'dark') {
    theme = darkTheme;
  } else if (readerSetting.theme.mode === 'system') {
    if (osThemeRef.value) {
      theme = osThemeRef.value === 'dark' ? darkTheme : null;
    }
  } else if (readerSetting.theme.mode === 'custom') {
    theme = isDarkColor(readerSetting.theme.bodyColor) ? darkTheme : null;
    bodyColor = readerSetting.theme.bodyColor;
  }

  return { theme, bodyColor: bodyColor ? { bodyColor } : undefined };
});
</script>

<template>
  <n-config-provider
    :theme="readerTheme.theme"
    :locale="zhCN"
    :date-locale="dateZhCN"
    inline-theme-disabled
    :theme-overrides="{
      common: {
        ...readerTheme.bodyColor,
      },
    }"
  >
    <n-global-style />
    <n-message-provider container-style="white-space: pre-wrap">
      <slot />
    </n-message-provider>
  </n-config-provider>
</template>
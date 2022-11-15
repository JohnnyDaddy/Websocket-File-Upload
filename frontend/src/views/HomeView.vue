<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-form
          ref="form"
          v-model="valid"
          lazy-validation
          :disabled="loading"
          @submit.prevent="onSubmit"
        >
          <v-card>
            <v-card-title> Multiple File Upload </v-card-title>
            <v-card-text>
              <v-file-input
                multiple
                v-model="files"
                :rules="[files.length > 0 || 'Requied']"
                label="File input"
              ></v-file-input>
            </v-card-text>
            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn
                color="primary"
                type="submit"
                :loading="loading"
                :disabled="!valid || loading"
              >
                Upload
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-form>
      </v-col>
    </v-row>
    <v-row v-if="files.length > 0">
      <v-col cols="12">
        <v-card>
          <v-card-title> Progress </v-card-title>
          <v-card-text>
            <ol>
              <li v-for="(file, index) in files" :key="index">
                {{ file.name }} ({{ file.size }} bytes)
                <v-progress-linear :value="getProgress(file.name)" height="20">
                  <strong>{{ getProgress(file.name) }}%</strong>
                </v-progress-linear>
              </li>
            </ol>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
export default {
  name: "HomeView",
  data: function () {
    return {
      loading: false,
      valid: true,
      files: [],
      knowledge: 20,
      worker: null,
      progresses: [],
    };
  },
  created: function () {
    const vm = this;
    this.worker = new Worker("/worker/fileupload.js");

    this.worker.onmessage = function (e) {
      const data = e.data;

      const file = vm.files.find((f) => f.name == data.originalFileName);
      if (file) {
        const rate = Math.ceil((data.receivedBytes / file.size) * 100);
        const progress = vm.progresses.find((p) => p.fileName == file.name);

        if (progress) {
          progress.rate = rate;
        } else {
          vm.progresses.push({
            fileName: file.name,
            rate: rate,
          });
        }
      }

      if (data.command == "complete") {
        vm.loading = false;
      }
    };

    this.worker.onerror = (e) => {
      console.error(
        "ERROR: Line ",
        e.lineno,
        " in ",
        e.filename,
        ": ",
        e.message
      );
    };
  },
  methods: {
    getProgress: function (fileName) {
      let result = 0;
      const progress = this.progresses.find((p) => p.fileName == fileName);
      if (progress) {
        result = progress.rate;
      }
      return result;
    },
    onSubmit: function () {
      if (this.$refs.form.validate()) {
        this.loading = true;

        try {
          this.worker.postMessage({
            files: this.files,
          });
        } catch (e) {
          console.error("Can't spawn files to worker - " + e);
        }
      }
    },
  },
};
</script>

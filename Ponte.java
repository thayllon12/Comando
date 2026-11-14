package com.comandante.player;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ponte {

    private final MainActivity atividade;
    private final ExecutorService exec = Executors.newFixedThreadPool(3);

    Ponte(MainActivity atividade) {
        this.atividade = atividade;
    }

    @JavascriptInterface
    public void abrirSeletor() {
        atividade.runOnUiThread(atividade::abrirSeletor);
    }

    @JavascriptInterface
    public void buscar(final String id, final String termo, final String fonte) {
        exec.execute(() -> {
            JSONObject o = new JSONObject();
            try {
                JSONArray itens = Busca.pesquisar(atividade, termo, fonte);
                o.put("id", id);
                o.put("ok", true);
                o.put("itens", itens);
            } catch (Exception e) {
                try {
                    o.put("id", id);
                    o.put("ok", false);
                    o.put("erro", Busca.mensagemErro(e));
                } catch (Exception ignorado) {
                }
            }
            atividade.responder("busca", o);
        });
    }

    @JavascriptInterface
    public void adicionarBusca(final String json) {
        exec.execute(() -> {
            JSONObject resposta = new JSONObject();
            String id = "";
            try {
                JSONObject t = new JSONObject(json);
                id = t.getString("id");
                String previa = t.getString("previa");
                String capaUrl = t.optString("capa", "");
                String nome = Arquivos.seguro(id);
                File dir = Arquivos.pasta(atividade);
                String ext = Arquivos.seguro(t.optString("ext", "m4a"));
                if (ext.isEmpty()) ext = "m4a";
                File audio = new File(dir, nome + "." + ext);
                if (!audio.exists()) Busca.baixar(previa, audio);
                String capaLocal = "";
                if (!capaUrl.isEmpty()) {
                    File c = new File(dir, nome + ".jpg");
                    try {
                        if (!c.exists()) Busca.baixar(capaUrl, c);
                        capaLocal = Arquivos.BASE + "/musicas/" + c.getName();
                    } catch (Exception ignorado) {
                    }
                }
                JSONObject faixa = new JSONObject();
                faixa.put("id", id);
                faixa.put("titulo", t.optString("titulo"));
                faixa.put("artista", t.optString("artista"));
                faixa.put("capa", capaLocal);
                faixa.put("url", Arquivos.BASE + "/musicas/" + audio.getName());
                faixa.put("duracao", t.optLong("duracao", 0));
                faixa.put("origem", "busca");
                resposta.put("id", id);
                resposta.put("ok", true);
                resposta.put("faixa", faixa);
            } catch (Exception e) {
                try {
                    resposta.put("id", id);
                    resposta.put("ok", false);
                    resposta.put("erro", Busca.mensagemErro(e));
                } catch (Exception ignorado) {
                }
            }
            atividade.responder("adicionado", resposta);
        });
    }

    @JavascriptInterface
    public void removerArquivos(String jsonArray) {
        try {
            JSONArray a = new JSONArray(jsonArray);
            File dir = Arquivos.pasta(atividade);
            for (int i = 0; i < a.length(); i++) {
                String nome = new File(a.getString(i)).getName();
                new File(dir, nome).delete();
            }
        } catch (Exception ignorado) {
        }
    }

    @JavascriptInterface
    public void atualizarMidia(String titulo, String artista, String capa, double duracaoMs, double posicaoMs, boolean tocando) {
        PlayerService.enviar(atividade.getApplicationContext(), titulo, artista, capa, (long) duracaoMs, (long) posicaoMs, tocando);
    }

    @JavascriptInterface
    public void vibrar(String padrao) {
        try {
            String[] p = padrao.split(",");
            int n = Math.min(p.length, 40);
            long[] w = new long[n];
            for (int i = 0; i < n; i++) w[i] = Math.max(0, Math.min(1000, Long.parseLong(p[i].trim())));
            Vibrator v;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) atividade.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm.getDefaultVibrator();
            } else {
                v = (Vibrator) atividade.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null || !v.hasVibrator()) return;
            v.vibrate(VibrationEffect.createWaveform(w, -1));
        } catch (Exception ignorado) {
        }
    }

    @JavascriptInterface
    public void encerrarMidia() {
        PlayerService.encerrar(atividade.getApplicationContext());
    }
}

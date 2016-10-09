package com.ingenic.ancsclient;

import android.content.Context;

import java.io.FileInputStream;

public class PowerWorker {
    private PowerWorkerCallback m_callback;
    private PowerPoller m_poller;
    private Object m_argument;

    public PowerWorker(Context context, PowerWorkerCallback callback,
            Object argument) {
        m_callback = callback;
        m_poller = new PowerPoller();
        m_poller.setName("PwrWork");
        m_argument = argument;

        m_poller.start();
    }

    // TODO: quit the thread immediately
    public void quit() {
        AncsLog.d("quit PowerWorker");
        m_poller.quit();
    }

    public interface PowerWorkerCallback {
        public void suspend(Object arg);

        public void resume(Object arg);
    }

    private class PowerPoller extends Thread {
        private FileInputStream wakeBlockUntilLast = null;
        private FileInputStream waitResume = null;
        private boolean quit = false;

        private byte[] bufForSuspend = new byte[50];
        private byte[] bufForResume = new byte[50];

        private void waitForSuspend() {
            try {
                if (wakeBlockUntilLast == null)
                    wakeBlockUntilLast = new FileInputStream(
                            "/sys/power/wake_block_until_last");

                try {
                    wakeBlockUntilLast.read(bufForSuspend);
                } catch (Exception e) {
                    /*
                     * Ignore
                     */
                }
            } catch (Exception e) {
                /*
                 * Ignore
                 */
            }
        }

        private void waitForResume() {
            try {
                if (waitResume == null)
                    waitResume = new FileInputStream(
                            "/sys/power/wait_for_resume");

                try {
                    waitResume.read(bufForResume);
                } catch (Exception e) {
                    /*
                     * Ignore
                     */
                }
            } catch (Exception e) {
                /*
                 * Ignore
                 */
            }
        }

        synchronized void quit() {
            quit = true;
        }

        synchronized boolean needToQuit() {
            return quit;
        }
        @Override
        public void run() {
            for (;;) {
                if (needToQuit())
                    break;

                AncsLog.d("Do something after resuming.");
                m_callback.resume(m_argument);

                waitForSuspend();

                AncsLog.d("Do something before suspending.");
                m_callback.suspend(m_argument);

                waitForResume();
            }
        }
    }
}

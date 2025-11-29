package com.maxprograms.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class CustomResponseWrapper extends HttpServletResponseWrapper {
    
    private int status = SC_OK;
    private boolean error;
    private String errorMessage;

    public CustomResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        this.status = sc;
    }

    @Override
    public void sendError(int sc) throws IOException {
        this.status = sc;
        this.error = true;
        this.errorMessage = null;
        if (sc == HttpServletResponse.SC_NOT_FOUND) {
            super.setStatus(sc);
            if (!isCommitted()) {
                super.resetBuffer();
            }
        } else {
            super.sendError(sc);
        }
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        this.status = sc;
        this.error = true;
        this.errorMessage = msg;
        if (sc == HttpServletResponse.SC_NOT_FOUND) {
            super.setStatus(sc);
            if (!isCommitted()) {
                super.resetBuffer();
            }
        } else {
            super.sendError(sc, msg);
        }
    }

    public int getStatus() {
        return status;
    }

    public boolean isErrorResponse() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

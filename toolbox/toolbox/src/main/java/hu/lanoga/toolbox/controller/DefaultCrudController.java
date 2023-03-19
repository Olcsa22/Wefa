package hu.lanoga.toolbox.controller;

import com.google.common.collect.Lists;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.export.ExporterException;
import hu.lanoga.toolbox.export.ExporterMananger;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.filter.input.angular.AngularPageRequest;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.json.jackson.JacksonHelper;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import hu.lanoga.toolbox.util.ToolboxAssert;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

public abstract class DefaultCrudController<T extends ToolboxPersistable, U extends ToolboxCrudService<T>> implements ToolboxCrudController<T> {

    @SuppressWarnings("unchecked")
    private Class<T> modelClass = (Class<T>) GenericTypeResolver.resolveTypeArguments(this.getClass(), DefaultCrudController.class)[0]; // elvileg csak Spring bean-ekre működik

    /**
     * ez a mindig épp aktuális HttpServletResponse (Spring injektálja, mivel nem Singleton ezért mindig az aktuális request contextnek megfelelő lesz)
     */
    @Autowired
    private HttpServletResponse currentHttpServletResponse;

    @Autowired
    private ExporterMananger exporterMananger;

    protected void handleExport(List<T> list, final String exportType) {

        try {

            Triple<FileDescriptor, String, String> export = null;
          
            Integer expType = null;

            try {

                expType = Integer.parseInt(exportType);

            } catch (Exception e) {

                if ("XLSX".equalsIgnoreCase(exportType)) {
                    expType = ToolboxSysKeys.ExportType.XLSX;
                } else if ("PDF".equalsIgnoreCase(exportType)) {
                    expType = ToolboxSysKeys.ExportType.PDF;
                }

            }
            
            ToolboxAssert.notNull(expType);

            export = exporterMananger.export(list, modelClass, expType);

            currentHttpServletResponse.setHeader("Content-Type", export.getRight());
            currentHttpServletResponse.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(export.getMiddle(), "UTF-8").replace("+", "%20"));

            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(export.getLeft().getFile()), 128 * 1024); OutputStream outputStream = currentHttpServletResponse.getOutputStream()) {
                IOUtils.copyLarge(inputStream, outputStream);
            }

        } catch (Exception e) {
            throw new ExporterException("Controller export error!", e);
        }

    }

    @Autowired
    protected U service;

    /**
     * nagyon ritkán kell csak spec. esetekben
     *
     * @return
     */
    public U getService() {
        return service;
    }

    @Override
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public T findOne(@PathVariable("id") final int id, @RequestParam(value = "exportType", required = false) final String exportType) {

        if (exportType != null) {
            handleExport(Lists.newArrayList(service.findOne(id)), exportType);
            return null;
        }

        return service.findOne(id);
    }

    @Override
    @RequestMapping(method = RequestMethod.GET)
    public List<T> findAll(@RequestParam(value = "exportType", required = false) final String exportType) {

        if (exportType != null) {
            handleExport(service.findAll(), exportType);
            return null;
        }

        return service.findAll();
    }

    @SuppressWarnings("unchecked")
	@Override
    @RequestMapping(value = {"/pagedexp", "/paged"}, method = RequestMethod.POST)
    public Page<T> findAllPaged(@RequestBody final String angularPageRequestAsJsonString, @RequestParam(value = "exportType", required = false) final String exportType) {

        AngularPageRequest<T> angularPageRequest = JacksonHelper.fromJson(angularPageRequestAsJsonString, AngularPageRequest.class);
        BasePageRequest<T> basePageRequest = angularPageRequest.getAsBasePageRequest();

        if (exportType != null) {
            handleExport(service.findAll(basePageRequest).getContent(), exportType);
            return null;
        }

        return service.findAll(basePageRequest);
    }

    /**
     * objektum id=null esetén insert, egyébként update...
     */
    @Override
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public T save(@RequestBody @Valid final T t) {
        return service.save(t);
    }

    @Override
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("id") final int id) {
        service.delete(id);
    }

}

package com.techsen.maven.plugins.version;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;

@Mojo(name = "study")
public class StudyMojo extends AbstractScmMojo {

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();

        try {
            StatusScmResult ssr = this.getScmManager().status(this.getScmRepository(), this.getFileSet());

            this.checkResult(ssr);

            System.out.println("******status start********");
            System.out.println(ssr.getCommandLine());
            System.out.println(ssr.getChangedFiles());
            System.out.println("******status end**********");
            
            CheckInScmResult csr = this.getScmManager().checkIn(this.getScmRepository(), this.getFileSet(), "checkin test");
            this.checkResult(csr);
            
            System.out.println("******checkIn start*******");
            System.out.println(csr.getCommandLine());
            System.out.println(csr.getCommandOutput());
            System.out.println(csr.getProviderMessage());
            System.out.println(csr.getScmRevision());
            System.out.println(csr.getCheckedInFiles());
            System.out.println("******checkIn start*******");

        } catch (ScmException | IOException e) {
            e.printStackTrace();
        }

    }

}
